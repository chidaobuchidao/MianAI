package com.mianmiantong.controller.paper;

import com.mianmiantong.dto.paper.PaperExportRequest;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.document.TemplatePreservingExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paper-export")
@RequiredArgsConstructor
public class PaperExportController {

    private final TemplatePreservingExportService exportService;
    private final ResumeMapper resumeMapper;

    @PostMapping("/preserve-format")
    public ResponseEntity<byte[]> exportPreserveFormat(
            @RequestBody PaperExportRequest request,
            @RequestParam Long resumeId,
            @RequestParam(required = false) String fileName) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getFileData() == null) {
            return ResponseEntity.badRequest().build();
        }

        byte[] result = exportService.exportWithPreservedFormat(resume.getFileData(), request);
        String downloadName = fileName != null ? fileName : "export";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + downloadName + ".docx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(result);
    }
}
