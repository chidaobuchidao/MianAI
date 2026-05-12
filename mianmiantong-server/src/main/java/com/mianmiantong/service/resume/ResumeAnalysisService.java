package com.mianmiantong.service.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.entity.resume.ResumeAnalysis;
import com.mianmiantong.mapper.resume.ResumeAnalysisMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.ai.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ResumeAnalysisService {

    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper analysisMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        你是一位资深HR和技术面试官，拥有10年以上的技术招聘经验。
        请对以下简历进行分析，目标岗位为：%s

        ## 分析维度

        1. **结构完整性** (1-10分)
        2. **技术关键词匹配度** (1-10分)
        3. **项目描述质量** (1-10分)
        4. **排版与可读性** (1-10分)
        5. **语言表达** (1-10分)

        ## 输出格式

        请严格按照以下JSON格式输出（不要包含markdown代码块标记）：

        {
          "overallScore": 7,
          "dimensions": [
            {"name": "结构完整性", "score": 7, "comment": "..."}
          ],
          "missingKeywords": ["关键词1", "关键词2"],
          "highlights": [
            {
              "section": "段落标题",
              "before": "优化前文本",
              "after": "优化后文本",
              "reason": "修改理由"
            }
          ],
          "optimizedText": "完整优化后的简历(Markdown格式)",
          "interviewQuestions": ["面试追问1", "面试追问2"],
          "suggestion": "总体提升建议(50-100字)"
        }

        ## 原始简历

        %s

        ## 注意事项
        - 输出JSON必须为一整行，不要换行
        - optimizedText 中的简历优化需保持事实不变，仅优化表达
        - interviewQuestions 需与简历内容真实相关
        """;

    public ResumeAnalysisService(ResumeMapper resumeMapper,
                                  ResumeAnalysisMapper analysisMapper,
                                  AiService aiService) {
        this.resumeMapper = resumeMapper;
        this.analysisMapper = analysisMapper;
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 流式 AI 分析
     */
    public SseEmitter analyzeStream(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getParseStatus() != 1) {
            throw new IllegalArgumentException("简历不存在或尚未解析完成");
        }

        String systemPrompt = String.format(SYSTEM_PROMPT,
            resume.getJobDescription(), resume.getParsedText());

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", "请开始分析")
        );

        SseEmitter emitter = new SseEmitter(180_000L);
        StringBuilder fullResponse = new StringBuilder();

        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("AI分析超时"));
            } catch (Exception ignored) {}
            emitter.complete();
        });

        CompletableFuture.runAsync(() -> {
            try {
                aiService.streamChat(systemPrompt, messages, null, token -> {
                    fullResponse.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (Exception e) {
                        throw new RuntimeException("SSE发送失败", e);
                    }
                });

                String aiResponse = fullResponse.toString();
                String jsonStr = extractJson(aiResponse);

                @SuppressWarnings("unchecked")
                Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);

                ResumeAnalysis analysis = new ResumeAnalysis();
                analysis.setResumeId(resumeId);
                analysis.setOverallScore(report.get("overallScore") != null
                    ? ((Number) report.get("overallScore")).intValue() : 0);
                analysis.setDimensions(toJson(report.get("dimensions")));
                analysis.setMissingKeywords(toJson(report.get("missingKeywords")));
                analysis.setOptimizedText((String) report.get("optimizedText"));
                analysis.setHighlights(toJson(report.get("highlights")));
                analysis.setInterviewQuestions(toJson(report.get("interviewQuestions")));
                analysis.setSuggestion((String) report.get("suggestion"));

                ResumeAnalysis existing = analysisMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                        .eq(ResumeAnalysis::getResumeId, resumeId)
                );
                if (existing != null) {
                    analysis.setId(existing.getId());
                    analysisMapper.updateById(analysis);
                } else {
                    analysisMapper.insert(analysis);
                }

                Map<String, Object> finishData = new LinkedHashMap<>();
                finishData.put("resumeId", resumeId);
                finishData.put("overallScore", analysis.getOverallScore());
                finishData.put("reportId", analysis.getId());

                emitter.send(SseEmitter.event().name("finish")
                    .data(objectMapper.writeValueAsString(finishData)));
                emitter.complete();

            } catch (Exception e) {
                log.error("AI分析异常: resumeId={}", resumeId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data("AI分析失败: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 获取已生成的分析报告
     */
    public Map<String, Object> getReport(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        ResumeAnalysis analysis = analysisMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
        );

        if (analysis == null) throw new IllegalArgumentException("分析报告不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumeId", resumeId);
        result.put("overallScore", analysis.getOverallScore());
        result.put("fileName", resume.getFileName());
        result.put("jobDescription", resume.getJobDescription());
        result.put("dimensions", parseJson(analysis.getDimensions()));
        result.put("missingKeywords", parseJson(analysis.getMissingKeywords()));
        result.put("highlights", parseJson(analysis.getHighlights()));
        result.put("optimizedText", analysis.getOptimizedText());
        result.put("interviewQuestions", parseJson(analysis.getInterviewQuestions()));
        result.put("suggestion", analysis.getSuggestion());
        return result;
    }

    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}") + 1;
        if (start >= 0 && end > start) {
            return response.substring(start, end);
        }
        return response;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "null"; }
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try { return objectMapper.readValue(json, Object.class); }
        catch (JsonProcessingException e) { return json; }
    }
}
