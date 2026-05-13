package com.mianmiantong.controller.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.common.JwtUtil;
import com.mianmiantong.common.Result;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.service.document.HtmlPreviewService;
import com.mianmiantong.service.document.TemplatePreservingExportService;
import com.mianmiantong.service.document.WordExportService;
import com.mianmiantong.service.resume.ResumeAnalysisService;
import com.mianmiantong.service.resume.ResumeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService analysisService;
    private final WordExportService wordExportService;
    private final TemplatePreservingExportService templateExportService;
    private final HtmlPreviewService htmlPreviewService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeController(ResumeService resumeService,
                           ResumeAnalysisService analysisService,
                           WordExportService wordExportService,
                           TemplatePreservingExportService templateExportService,
                           HtmlPreviewService htmlPreviewService,
                           JwtUtil jwtUtil) {
        this.resumeService = resumeService;
        this.analysisService = analysisService;
        this.wordExportService = wordExportService;
        this.templateExportService = templateExportService;
        this.htmlPreviewService = htmlPreviewService;
        this.jwtUtil = jwtUtil;
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
    @PostMapping("/{resumeId}/analyze")
    public Result<?> analyze(@PathVariable Long resumeId) {
        analysisService.analyzeQuickAsync(resumeId);
        return Result.ok(Map.of("message", "分析已开始"));
    }

    /** Phase 2: 深度优化 SSE 流式 */
    @PostMapping("/{resumeId}/analyze-deep")
    public SseEmitter analyzeDeep(@PathVariable Long resumeId,
                                  @RequestParam(value = "model", required = false) String model) {
        return analysisService.analyzeDeepStream(resumeId, model);
    }

    /** 检查重试状态并触发重试 */
    @PostMapping("/{resumeId}/retry-deep")
    public SseEmitter retryDeep(@PathVariable Long resumeId,
                                @RequestParam(value = "model", required = false) String model) {
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

    /** 导出优化简历为 Word（如有原始docx则保留原模板格式） */
    @GetMapping("/{resumeId}/export-word")
    @SuppressWarnings("unchecked")
    public void exportWord(@PathVariable Long resumeId, HttpServletResponse response) {
        var report = analysisService.getReport(resumeId);
        String optimizedText = (String) report.get("optimizedText");
        if (optimizedText == null || optimizedText.isBlank()) {
            throw new IllegalArgumentException("优化简历内容为空");
        }
        String fileName = String.valueOf(report.getOrDefault("fileName", "简历优化"));

        Resume resume = resumeService.getById(resumeId);
        byte[] docx;

        // 原文件是 docx 且有 highlights 时，使用模板保留导出
        if ("docx".equals(resume.getFileType()) && resume.getFileData() != null
                && report.get("highlights") instanceof List<?> highlights && !highlights.isEmpty()) {
            try {
                List<Map<String, Object>> hlList = (List<Map<String, Object>>) highlights;
                docx = templateExportService.exportWithHighlights(resume.getFileData(), hlList);
            } catch (Exception e) {
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

    /** 预览优化简历为 HTML（小程序 web-view 内使用） */
    @GetMapping("/{resumeId}/preview-html")
    public String previewHtml(@PathVariable Long resumeId, @RequestParam("token") String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("无效的访问令牌");
        }

        var report = analysisService.getReport(resumeId);
        String optimizedText = (String) report.get("optimizedText");
        if (optimizedText == null || optimizedText.isBlank()) {
            throw new IllegalArgumentException("优化简历内容为空");
        }

        Resume resume = resumeService.getById(resumeId);
        byte[] docx;

        if ("docx".equals(resume.getFileType()) && resume.getFileData() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hlList = (List<Map<String, Object>>) report.get("highlights");
                if (hlList != null && !hlList.isEmpty()) {
                    docx = templateExportService.exportWithHighlights(resume.getFileData(), hlList);
                } else {
                    docx = wordExportService.exportMarkdown(optimizedText,
                            String.valueOf(report.getOrDefault("fileName", "简历")));
                }
            } catch (Exception e) {
                docx = wordExportService.exportMarkdown(optimizedText,
                        String.valueOf(report.getOrDefault("fileName", "简历")));
            }
        } else {
            docx = wordExportService.exportMarkdown(optimizedText,
                    String.valueOf(report.getOrDefault("fileName", "简历")));
        }

        return htmlPreviewService.convertDocxToHtml(docx);
    }

    /** 删除简历 */
    @DeleteMapping("/{resumeId}")
    public Result<?> delete(@PathVariable Long resumeId) {
        resumeService.delete(resumeId);
        return Result.ok();
    }
}
