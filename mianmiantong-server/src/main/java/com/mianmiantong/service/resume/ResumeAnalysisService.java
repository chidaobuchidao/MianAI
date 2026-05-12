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

    /** Phase 1: 快速评分，轻量快速 */
    private static final String QUICK_PROMPT = """
        你是资深HR。快速评估这份简历（目标岗位：%s），输出JSON：
        {
          "overallScore": 7,
          "dimensions": [
            {"name":"结构完整性","score":7,"comment":"简评"},
            {"name":"关键词匹配","score":5,"comment":"简评"},
            {"name":"项目描述","score":6,"comment":"简评"},
            {"name":"排版可读","score":6,"comment":"简评"},
            {"name":"语言表达","score":7,"comment":"简评"}
          ],
          "missingKeywords": ["关键词1"],
          "suggestion": "一句话总体建议"
        }
        简历内容：
        %s
        要求：JSON一行，不要markdown标记。简评15字以内。
        """;

    /** Phase 2: 深度优化 */
    private static final String DEEP_PROMPT = """
        你是简历优化专家。基于以下简历和评分，进行深度优化（目标岗位：%s）：

        当前评分：%d/10，缺失关键词：%s

        简历内容：
        %s

        输出JSON（一行，不要markdown标记）：
        {
          "highlights": [
            {"section":"段落名","before":"原文","after":"优化文","reason":"理由"}
          ],
          "optimizedText": "完整优化后简历（Markdown）",
          "interviewQuestions": ["追问1","追问2","追问3"]
        }
        """;

    public ResumeAnalysisService(ResumeMapper resumeMapper,
                                  ResumeAnalysisMapper analysisMapper,
                                  AiService aiService) {
        this.resumeMapper = resumeMapper;
        this.analysisMapper = analysisMapper;
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /** Phase 1: 快速评分（异步后台） */
    public void analyzeQuickAsync(Long resumeId) {
        CompletableFuture.runAsync(() -> {
            try {
                Resume resume = resumeMapper.selectById(resumeId);
                if (resume == null || resume.getParseStatus() != 1) return;

                String prompt = String.format(QUICK_PROMPT, resume.getJobDescription(), resume.getParsedText());
                List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", "开始评估"));
                String response = aiService.chat(prompt, messages);
                Map<String, Object> report = objectMapper.readValue(extractJson(response), Map.class);

                ResumeAnalysis analysis = upsert(resumeId);
                analysis.setOverallScore(toInt(report.get("overallScore")));
                analysis.setDimensions(toJson(report.get("dimensions")));
                analysis.setMissingKeywords(toJson(report.get("missingKeywords")));
                analysis.setSuggestion((String) report.get("suggestion"));
                analysis.setDeepStatus(0);
                save(analysis);
                log.info("快速评分完成: resumeId={}, score={}", resumeId, analysis.getOverallScore());
            } catch (Exception e) {
                log.error("快速评分失败: resumeId={}", resumeId, e);
            }
        });
    }

    /** Phase 1: 快速评分 SSE（兼容旧版，保留） */
    public SseEmitter analyzeStream(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getParseStatus() != 1) {
            throw new IllegalArgumentException("简历不存在或尚未解析完成");
        }

        String prompt = String.format(QUICK_PROMPT, resume.getJobDescription(), resume.getParsedText());
        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", "开始评估"));

        SseEmitter emitter = new SseEmitter(120_000L);
        StringBuilder buf = new StringBuilder();

        emitter.onTimeout(() -> safeSend(emitter, "error", "分析超时"));

        CompletableFuture.runAsync(() -> {
            try {
                aiService.streamChat(prompt, messages, null, token -> {
                    buf.append(token);
                    safeSend(emitter, "token", token);
                });

                Map<String, Object> report = objectMapper.readValue(extractJson(buf.toString()), Map.class);

                ResumeAnalysis analysis = upsert(resumeId);
                analysis.setOverallScore(toInt(report.get("overallScore")));
                analysis.setDimensions(toJson(report.get("dimensions")));
                analysis.setMissingKeywords(toJson(report.get("missingKeywords")));
                analysis.setSuggestion((String) report.get("suggestion"));
                analysis.setDeepStatus(0); // 待深度优化
                save(analysis);

                emitter.send(SseEmitter.event().name("finish")
                        .data(objectMapper.writeValueAsString(Map.of(
                                "resumeId", resumeId, "overallScore", analysis.getOverallScore(),
                                "phase", "quick"))));
                emitter.complete();
            } catch (Exception e) {
                log.error("快速分析失败: resumeId={}", resumeId, e);
                safeSend(emitter, "error", e.getMessage());
                emitter.complete();
            }
        });
        return emitter;
    }

    /** Phase 2: 深度优化，异步后台执行 */
    public void analyzeDeepAsync(Long resumeId) {
        CompletableFuture.runAsync(() -> {
            try {
                Resume resume = resumeMapper.selectById(resumeId);
                ResumeAnalysis analysis = upsert(resumeId);

                analysis.setDeepStatus(1); // 进行中
                save(analysis);

                String keywords = analysis.getMissingKeywords() != null ? analysis.getMissingKeywords() : "无";
                String prompt = String.format(DEEP_PROMPT, resume.getJobDescription(),
                        analysis.getOverallScore() != null ? analysis.getOverallScore() : 5,
                        keywords, resume.getParsedText());

                List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", "开始深度优化"));
                String response = aiService.chat(prompt, messages);

                Map<String, Object> report = objectMapper.readValue(extractJson(response), Map.class);
                analysis.setHighlights(toJson(report.get("highlights")));
                analysis.setOptimizedText((String) report.get("optimizedText"));
                analysis.setInterviewQuestions(toJson(report.get("interviewQuestions")));
                analysis.setDeepStatus(2); // 完成
                save(analysis);
                log.info("深度优化完成: resumeId={}", resumeId);
            } catch (Exception e) {
                log.error("深度优化失败: resumeId={}", resumeId, e);
                try {
                    ResumeAnalysis analysis = upsert(resumeId);
                    analysis.setDeepStatus(-1);
                    save(analysis);
                } catch (Exception ignored) {}
            }
        });
    }

    /** 获取深度优化状态 */
    public Map<String, Object> getDeepStatus(Long resumeId) {
        ResumeAnalysis analysis = analysisMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                        .eq(ResumeAnalysis::getResumeId, resumeId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deepStatus", analysis != null ? analysis.getDeepStatus() : null);
        return result;
    }

    public Map<String, Object> getReport(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        ResumeAnalysis analysis = analysisMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                        .eq(ResumeAnalysis::getResumeId, resumeId));
        if (analysis == null) throw new IllegalArgumentException("分析报告不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumeId", resumeId);
        result.put("overallScore", analysis.getOverallScore());
        result.put("fileName", resume.getFileName());
        result.put("jobDescription", resume.getJobDescription());
        result.put("dimensions", parseJson(analysis.getDimensions()));
        result.put("missingKeywords", parseJson(analysis.getMissingKeywords()));
        result.put("suggestion", analysis.getSuggestion());
        result.put("deepStatus", analysis.getDeepStatus());
        // 深度优化结果（可能为空）
        result.put("highlights", parseJson(analysis.getHighlights()));
        result.put("optimizedText", analysis.getOptimizedText());
        result.put("interviewQuestions", parseJson(analysis.getInterviewQuestions()));
        return result;
    }

    private ResumeAnalysis upsert(Long resumeId) {
        ResumeAnalysis existing = analysisMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                        .eq(ResumeAnalysis::getResumeId, resumeId));
        if (existing != null) return existing;
        ResumeAnalysis a = new ResumeAnalysis();
        a.setResumeId(resumeId);
        analysisMapper.insert(a);
        return a;
    }

    private void save(ResumeAnalysis a) {
        if (a.getId() != null) analysisMapper.updateById(a);
        else analysisMapper.insert(a);
    }

    private void safeSend(SseEmitter e, String name, String data) {
        try { e.send(SseEmitter.event().name(name).data(data)); } catch (Exception ignored) {}
    }

    private String extractJson(String s) {
        int a = s.indexOf("{"), b = s.lastIndexOf("}") + 1;
        return (a >= 0 && b > a) ? s.substring(a, b) : s;
    }

    private int toInt(Object v) { return v instanceof Number n ? n.intValue() : 0; }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { return "null"; }
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try { return objectMapper.readValue(json, Object.class); } catch (JsonProcessingException e) { return json; }
    }
}
