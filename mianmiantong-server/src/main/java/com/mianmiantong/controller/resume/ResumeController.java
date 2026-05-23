package com.mianmiantong.controller.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.user.UserAiConfigMapper;
import com.mianmiantong.mapper.user.UserMapper;
import java.time.LocalDate;
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
    private final UserMapper userMapper;
    private final UserAiConfigMapper aiConfigMapper;

    public ResumeController(ResumeService resumeService,
                           ResumeAnalysisService analysisService,
                           WordExportService wordExportService,
                           TemplatePreservingExportService templateExportService,
                           HtmlPreviewService htmlPreviewService,
                           JwtUtil jwtUtil,
                           UserMapper userMapper,
                           UserAiConfigMapper aiConfigMapper) {
        this.resumeService = resumeService;
        this.analysisService = analysisService;
        this.wordExportService = wordExportService;
        this.templateExportService = templateExportService;
        this.htmlPreviewService = htmlPreviewService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.aiConfigMapper = aiConfigMapper;
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
    /** 校验并消耗配额。配额不足抛异常。Flash=1, Pro=2 */
    private void consumeResumeQuota(String model) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        if (userId == null || JwtAuthFilter.isAdmin()) return;
        UserAiConfig config = aiConfigMapper.selectById(userId);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) return;
        var user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : 10;
        int used = user.getQuotaUsed() != null ? user.getQuotaUsed() : 0;
        // Reset if new day
        if (!java.time.LocalDate.now().equals(user.getQuotaDate())) { used = 0; }
        if (used >= daily) {
            throw new IllegalArgumentException("今日免费次数已用完（" + daily + "次/天），请配置 AI API Key 后无限使用");
        }
        int steps = (model != null && model.toLowerCase().contains("pro")) ? 2 : 1;
        userMapper.incrementQuota(userId, steps);
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

        // 直接使用优化后的 Markdown 生成 Word，不尝试保留原模板
        // 模板保留需要 AI 的 before 文本与原始文档精确匹配，实际很难做到
        // 用户通过页面上的 Diff 对比视图查看具体修改点
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
