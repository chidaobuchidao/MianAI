package com.mianmiantong.controller.resume;

import com.mianmiantong.common.Result;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.service.resume.ResumeService;
import com.mianmiantong.service.resume.TemplateResumeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/resume/template")
public class TemplateController {

    private final TemplateResumeService templateResumeService;
    private final ResumeService resumeService;

    public TemplateController(TemplateResumeService templateResumeService,
                             ResumeService resumeService) {
        this.templateResumeService = templateResumeService;
        this.resumeService = resumeService;
    }

    /** 获取模板列表 */
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(templateResumeService.listTemplates());
    }

    /** 使用模板生成简历（从已有简历） */
    @GetMapping("/generate")
    public void generate(@RequestParam Long resumeId, @RequestParam Long templateId,
                         HttpServletResponse response) {

        Resume resume = resumeService.getById(resumeId);
        byte[] docx = templateResumeService.generateFromResume(
                resumeId, templateId,
                resume.getParsedText() != null ? resume.getParsedText() : "",
                resume.getJobDescription()
        );

        String fileName = resume.getFileName().replaceAll("\\.[^.]+$", "") + "_模板版.docx";
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(docx.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(docx);
        } catch (Exception e) {
            throw new RuntimeException("生成失败", e);
        }
    }
}
