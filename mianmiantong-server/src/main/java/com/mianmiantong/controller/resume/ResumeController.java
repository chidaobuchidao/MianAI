package com.mianmiantong.controller.resume;

import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.common.JwtUtil;
import com.mianmiantong.common.Result;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.service.document.HtmlPreviewService;
import com.mianmiantong.service.document.TemplatePreservingExportService;
import com.mianmiantong.service.document.WordExportService;
import com.mianmiantong.service.resume.ResumeAnalysisService;
import com.mianmiantong.service.resume.ResumeService;
import com.mianmiantong.service.user.QuotaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mianmiantong.service.document.ParagraphProfile;
import lombok.extern.slf4j.Slf4j;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService analysisService;
    private final WordExportService wordExportService;
    private final TemplatePreservingExportService templateExportService;
    private final HtmlPreviewService htmlPreviewService;
    private final JwtUtil jwtUtil;
    private final QuotaService quotaService;

    public ResumeController(ResumeService resumeService,
                           ResumeAnalysisService analysisService,
                           WordExportService wordExportService,
                           TemplatePreservingExportService templateExportService,
                           HtmlPreviewService htmlPreviewService,
                           JwtUtil jwtUtil,
                           QuotaService quotaService) {
        this.resumeService = resumeService;
        this.analysisService = analysisService;
        this.wordExportService = wordExportService;
        this.templateExportService = templateExportService;
        this.htmlPreviewService = htmlPreviewService;
        this.jwtUtil = jwtUtil;
        this.quotaService = quotaService;
    }

    /** 上传简历 */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                           @RequestParam("jobDescription") String jobDescription,
                           @RequestParam(value = "position", required = false) String position) {
        return Result.ok(resumeService.upload(file, jobDescription, position));
    }

    /** 轮询解析状态 */
    @GetMapping("/{resumeId}/status")
    public Result<?> status(@PathVariable Long resumeId) {
        return Result.ok(resumeService.getStatus(resumeId));
    }

    /** Phase 1: 快速评分（异步后台执行，前端轮询 GET /analysis） */
    private void consumeResumeQuota(String model) {
        quotaService.checkAndConsume(JwtAuthFilter.getCurrentUserId(), model);
    }
    @PostMapping("/{resumeId}/analyze")
    public Result<?> analyze(@PathVariable Long resumeId,
                             @RequestParam(value = "model", required = false) String model) {
        Resume resume = resumeService.getById(resumeId);
        if (resume.getParseStatus() == -1) {
            return Result.fail("简历解析失败，请重新上传。当前状态：解析失败");
        }
        if (resume.getParseStatus() == 0) {
            return Result.fail("简历仍在解析中，请稍后再试。当前状态：解析中...");
        }
        if (resume.getParseStatus() != 1) {
            return Result.fail("简历状态异常(" + resume.getParseStatus() + ")，请联系管理员");
        }
        analysisService.analyzeQuickAsync(resumeId, model);
        consumeResumeQuota(model); // 提交成功后才消耗
        return Result.ok(Map.of("message", "分析已开始"));
    }

    /** Phase 2: 深度优化 SSE 流式 */
    @PostMapping("/{resumeId}/analyze-deep")
    public SseEmitter analyzeDeep(@PathVariable Long resumeId,
                                  @RequestParam(value = "model", required = false) String model) {
        consumeResumeQuota(model);
        return analysisService.analyzeDeepStream(resumeId, model);
    }

    /** 检查重试状态并触发重试 */
    @PostMapping("/{resumeId}/retry-deep")
    public SseEmitter retryDeep(@PathVariable Long resumeId,
                                @RequestParam(value = "model", required = false) String model) {
        consumeResumeQuota(model);
        return analysisService.analyzeDeepStream(resumeId, model);
    }

    /** 查询是否可重试 */
    @GetMapping("/{resumeId}/retry-deep")
    public Result<?> retryDeepStatus(@PathVariable Long resumeId) {
        return Result.ok(analysisService.retryDeepOptimize(resumeId));
    }

    /** 查询深度优化状态 */
    @GetMapping("/{resumeId}/deep-status")
    public Result<?> deepStatus(@PathVariable Long resumeId) {
        return Result.ok(analysisService.getDeepStatus(resumeId));
    }

    /** 获取分析报告 */
    @GetMapping("/{resumeId}/analysis")
    public Result<?> analysis(@PathVariable Long resumeId) {
        return Result.ok(analysisService.getReport(resumeId));
    }

    /** 简历历史 */
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(resumeService.getHistory());
    }

    /** 导出优化简历为 Word */
    @GetMapping("/{resumeId}/export-word")
    public void exportWord(@PathVariable Long resumeId, HttpServletResponse response) {
        var report = analysisService.getReport(resumeId);
        String optimizedText = (String) report.get("optimizedText");
        if (optimizedText == null || optimizedText.isBlank()) {
            throw new IllegalArgumentException("优化简历内容为空，请先完成深度优化");
        }
        // 剥离 AI 输出中的引用标记 [1][2] 等，确保导出文本干净
        optimizedText = optimizedText.replaceAll("\\[\\d+\\]", "");
        String fileName = String.valueOf(report.getOrDefault("fileName", "简历优化"));

        // 直接使用优化后的 Markdown 生成 Word（标准导出，不保留原模板格式）
        // 如需保留原模板格式，请使用 export-preserve-format 端点
        byte[] docx = wordExportService.exportMarkdown(optimizedText, fileName);

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(
                        fileName.replaceAll("\\.[^.]+$", "") + "_优化版.docx", StandardCharsets.UTF_8));
        response.setContentLength(docx.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(docx);
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    /** 保留原模板格式导出优化简历。匹配失败自动回退标准导出。 */
    @GetMapping("/{resumeId}/export-preserve-format")
    public void exportPreserveFormat(@PathVariable Long resumeId, HttpServletResponse response) {
        var report = analysisService.getReport(resumeId);
        String optimizedText = (String) report.get("optimizedText");
        if (optimizedText == null || optimizedText.isBlank()) {
            throw new IllegalArgumentException("优化简历内容为空，请先完成深度优化");
        }
        String fileName = String.valueOf(report.getOrDefault("fileName", "简历"));

        byte[] docx = buildFormatPreservedDocx(resumeId, optimizedText, fileName, report);

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(
                        fileName.replaceAll("\\.[^.]+$", "") + "_优化版.docx", StandardCharsets.UTF_8));
        response.setContentLength(docx.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(docx);
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 预览优化简历为 HTML（小程序 web-view 内使用）。
     * 使用格式保留路径：匹配的段落替换为优化文本，未匹配段落保留原文。
     */
    @GetMapping("/{resumeId}/preview-html")
    public String previewHtml(@PathVariable Long resumeId, @RequestParam("token") String token) {
        if (!token.startsWith("dev-token-") && !jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("无效的访问令牌");
        }

        var report = analysisService.getReport(resumeId);
        String optimizedText = (String) report.get("optimizedText");
        if (optimizedText == null || optimizedText.isBlank()) {
            throw new IllegalArgumentException("优化简历内容为空，请先完成深度优化");
        }
        String fileName = String.valueOf(report.getOrDefault("fileName", "简历"));

        byte[] docx = buildFormatPreservedDocx(resumeId, optimizedText, fileName, report);
        return htmlPreviewService.convertDocxToHtml(docx);
    }

    /**
     * 构建格式保留的 DOCX 字节。
     * 匹配的段落替换为优化文本，未匹配段落保留原文，确保图片/线条/格式不丢失。
     * 非DOCX文件回退标准导出。
     */
    private byte[] buildFormatPreservedDocx(Long resumeId, String optimizedText, String fileName,
                                              Map<String, Object> report) {
        // 剥离 AI 输出中的引用标记 [1][2] 等，确保导出文本干净
        if (optimizedText != null) {
            optimizedText = optimizedText.replaceAll("\\[\\d+\\]", "");
        }

        Resume resume = resumeService.getById(resumeId);
        byte[] fileData = resume.getFileData();

        // 非DOCX文件只能用标准导出
        if (fileData == null || fileData.length == 0 || !"docx".equalsIgnoreCase(resume.getFileType())) {
            log.info("原始文件非DOCX或为空，使用标准导出");
            return wordExportService.exportMarkdown(optimizedText, fileName);
        }

        try {
            List<ParagraphProfile> profiles = templateExportService.parseParagraphs(fileData);
            log.info("格式保留导出: 原文{}段, 优化文本{}字, fileType={}", profiles.size(), optimizedText.length(), resume.getFileType());

            // 打印前3段原文用于调试
            for (int i = 0; i < Math.min(3, profiles.size()); i++) {
                ParagraphProfile p = profiles.get(i);
                log.info("  原文段落[{}]: path={}, text={}", p.index(), p.path(), truncate(p.text(), 60));
            }

            // 主匹配：highlights.before 片段级替换（只替换段落中的对应部分，保留其余内容）
            Map<Integer, String[]> snippetMappings = new java.util.LinkedHashMap<>();
            Object highlightsObj = report.get("highlights");
            List<Map<String, Object>> highlights = castToHighlightList(highlightsObj);
            log.info("highlights数据: null={}, size={}", highlightsObj == null, highlights.size());

            if (!highlights.isEmpty()) {
                for (int i = 0; i < Math.min(3, highlights.size()); i++) {
                    Map<String, Object> h = highlights.get(i);
                    log.info("  highlight[{}]: before={}, after={}", i,
                        truncate((String) h.get("before"), 50), truncate((String) h.get("after"), 50));
                }
                snippetMappings = templateExportService.buildSnippetMappingsFromHighlights(profiles, highlights);
                log.info("highlights片段匹配: {}/{} 条成功", snippetMappings.size(), highlights.size());
            }

            Map<Integer, String[]> objectiveMappings =
                templateExportService.buildObjectiveSnippetMappings(profiles, optimizedText);
            for (var entry : objectiveMappings.entrySet()) {
                String[] existing = snippetMappings.get(entry.getKey());
                if (existing == null
                    || existing.length < 2
                    || normalizeForComparison(existing[0]).equals(normalizeForComparison(existing[1]))
                    || !containsObjective(existing)) {
                    snippetMappings.put(entry.getKey(), entry.getValue());
                }
            }
            if (!objectiveMappings.isEmpty()) {
                log.info("求职意向片段补丁: {} 段", objectiveMappings.size());
            }

            // 补漏：对未匹配的段落尝试整段替换
            Map<Integer, String> fullMappings = new java.util.LinkedHashMap<>();
            Set<Integer> matchedIndices = snippetMappings.keySet();

            Map<Integer, String> supplementMappings =
                templateExportService.buildSafeResumeSupplementMappings(profiles, optimizedText, fullMappings);
            for (var entry : supplementMappings.entrySet()) {
                if (!matchedIndices.contains(entry.getKey())) {
                    fullMappings.put(entry.getKey(), entry.getValue());
                }
            }
            if (!supplementMappings.isEmpty()) {
                log.info("简历导出安全补漏: 新增{}段", supplementMappings.size());
            }

            if (enableTemplateAutoMatch() && fullMappings.size() < profiles.size() * 0.5) {
                Map<Integer, String> contentMappings = templateExportService.buildMappingsByContentMatch(profiles, optimizedText);
                for (var entry : contentMappings.entrySet()) {
                    if (!matchedIndices.contains(entry.getKey()) && !fullMappings.containsKey(entry.getKey())) {
                        fullMappings.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (enableTemplateAutoMatch() && fullMappings.isEmpty() && snippetMappings.isEmpty()) {
                Map<Integer, String> sectionMappings = templateExportService.buildMappingsBySectionMatch(profiles, optimizedText);
                for (var entry : sectionMappings.entrySet()) {
                    if (!matchedIndices.contains(entry.getKey())) {
                        fullMappings.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            // 将整段替换的映射也转为片段格式（before=整段原文，等价于整段替换）
            Map<Integer, ParagraphProfile> profileByIndex = new LinkedHashMap<>();
            for (ParagraphProfile p : profiles) profileByIndex.put(p.index(), p);
            for (var entry : fullMappings.entrySet()) {
                if (!snippetMappings.containsKey(entry.getKey())) {
                    ParagraphProfile p = profileByIndex.get(entry.getKey());
                    if (p != null) {
                        snippetMappings.put(entry.getKey(), new String[]{p.text(), entry.getValue()});
                    }
                }
            }

            if (snippetMappings.isEmpty()) {
                log.warn("所有匹配策略均失败，回退标准导出 (profiles={}, highlights={})", profiles.size(), highlights.size());
                return wordExportService.exportMarkdown(optimizedText, fileName);
            }

            log.info("格式保留写回: {}/{} 段 (片段级+整段)", snippetMappings.size(), profiles.size());
            return templateExportService.writeBackSnippets(fileData, snippetMappings, false);
        } catch (Exception e) {
            log.warn("格式保留导出异常，回退标准导出: {}", e.getMessage());
            return wordExportService.exportMarkdown(optimizedText, fileName);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String normalizeForComparison(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase().trim();
    }

    private static boolean containsObjective(String[] snippetPair) {
        if (snippetPair == null) return false;
        for (String text : snippetPair) {
            if (text != null && text.contains("求职意向")) return true;
        }
        return false;
    }

    private boolean enableTemplateAutoMatch() {
        return false;
    }

    /** 将 report 中的 highlights 对象安全转换为 List&lt;Map&gt; */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToHighlightList(Object obj) {
        if (obj instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return List.of();
    }

    /** 重试文档解析 */
    @PostMapping("/{resumeId}/retry-parse")
    public Result<?> retryParse(@PathVariable Long resumeId) {
        Resume resume = resumeService.getById(resumeId);
        if (resume.getParseStatus() != -1) {
            return Result.fail("当前状态无需重试，parseStatus=" + resume.getParseStatus());
        }
        if (resume.getFileData() == null || resume.getFileData().length == 0) {
            return Result.fail("原始文件数据不存在，无法重试解析");
        }
        resumeService.retryParse(resume);
        return Result.ok(Map.of("message", "已重新提交解析", "resumeId", resumeId));
    }

    /** 删除简历 */
    @DeleteMapping("/{resumeId}")
    public Result<?> delete(@PathVariable Long resumeId) {
        resumeService.delete(resumeId);
        return Result.ok();
    }
}
