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
import java.util.List;
import java.util.Map;

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
    public Result<?> analyze(@PathVariable Long resumeId) {
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
        analysisService.analyzeQuickAsync(resumeId);
        consumeResumeQuota(null); // 提交成功后才消耗
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

    /** 保留原模板格式导出优化简历。使用语义匹配定位段落，匹配失败自动回退标准导出。 */
    @GetMapping("/{resumeId}/export-preserve-format")
    public void exportPreserveFormat(@PathVariable Long resumeId, HttpServletResponse response) {
        Resume resume = resumeService.getById(resumeId);
        var report = analysisService.getReport(resumeId);
        String optimizedText = (String) report.get("optimizedText");
        if (optimizedText == null || optimizedText.isBlank()) {
            throw new IllegalArgumentException("优化简历内容为空，请先完成深度优化");
        }
        String fileName = String.valueOf(report.getOrDefault("fileName", "简历"));

        byte[] docx;
        byte[] fileData = resume.getFileData();
        if (fileData != null && fileData.length > 0 && "docx".equalsIgnoreCase(resume.getFileType())) {
            try {
                List<ParagraphProfile> profiles = templateExportService.parseParagraphs(fileData);

                // 获取 AI 返回的 highlights（含 before/after 对照文本）
                Object highlightsObj = report.get("highlights");
                List<Map<String, Object>> highlights = castToHighlightList(highlightsObj);

                // 语义匹配：用 before 文本在 DOCX 中模糊定位段落（仅 highlights，不碰未标记段落）
                Map<Integer, String> mappings = templateExportService.buildMappingsFromHighlights(profiles, highlights);

                if (mappings.isEmpty()) {
                    log.warn("语义匹配未找到任何对应段落，回退标准导出");
                    docx = wordExportService.exportMarkdown(optimizedText, fileName);
                } else {
                    docx = templateExportService.writeBack(fileData, mappings, true);
                }
            } catch (Exception e) {
                log.warn("保留格式导出失败，回退标准导出: {}", e.getMessage());
                docx = wordExportService.exportMarkdown(optimizedText, fileName);
            }
        } else {
            docx = wordExportService.exportMarkdown(optimizedText, fileName);
        }

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

    /** 预览优化简历为 HTML（小程序 web-view 内使用） */
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

        // 直接从优化后的 Markdown 生成 HTML 预览
        String fileName = String.valueOf(report.getOrDefault("fileName", "简历"));
        byte[] docx = wordExportService.exportMarkdown(optimizedText, fileName);
        return htmlPreviewService.convertDocxToHtml(docx);
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
