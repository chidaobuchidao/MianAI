package com.mianmiantong.controller.resume;

import com.mianmiantong.common.Result;
import com.mianmiantong.service.resume.ResumeAnalysisService;
import com.mianmiantong.service.resume.ResumeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService analysisService;

    public ResumeController(ResumeService resumeService,
                           ResumeAnalysisService analysisService) {
        this.resumeService = resumeService;
        this.analysisService = analysisService;
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

    /** AI 分析 (SSE流式) */
    @PostMapping("/{resumeId}/analyze")
    public SseEmitter analyze(@PathVariable Long resumeId) {
        return analysisService.analyzeStream(resumeId);
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

    /** 删除简历 */
    @DeleteMapping("/{resumeId}")
    public Result<?> delete(@PathVariable Long resumeId) {
        resumeService.delete(resumeId);
        return Result.ok();
    }
}
