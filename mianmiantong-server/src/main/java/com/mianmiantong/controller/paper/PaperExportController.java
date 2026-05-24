package com.mianmiantong.controller.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.dto.paper.PaperExportRequest;
import com.mianmiantong.service.document.TemplatePreservingExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 格式保留导出控制器。
 * 接收原始文件 + 改写后的段落映射，在服务端处理完毕后立即丢弃文件，不做任何持久化。
 */
@Slf4j
@RestController
@RequestMapping("/api/paper-export")
@RequiredArgsConstructor
public class PaperExportController {

    private final TemplatePreservingExportService exportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 格式保留导出。
     * 前端将原始 DOCX 文件 + 段落映射 JSON 一并提交，服务端处理后立即丢弃文件。
     */
    @PostMapping("/preserve-format")
    public ResponseEntity<?> exportPreserveFormat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mappings") String mappingsJson) {

        try {
            byte[] fileBytes = file.getBytes();
            PaperExportRequest request = objectMapper.readValue(mappingsJson, PaperExportRequest.class);

            byte[] result = exportService.exportWithPreservedFormat(fileBytes, request);
            String fileName = request.getFileName() != null ? request.getFileName() : "export";

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + ".docx\"")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(result);
        } catch (Exception e) {
            log.error("格式保留导出失败", e);
            return ResponseEntity.internalServerError()
                .body("{\"error\":\"导出失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 标准导出 — 纯文本生成 DOCX（不保留原格式，回退方案）。
     * 不需要原始文件。
     */
    @PostMapping("/standard")
    public ResponseEntity<?> exportStandard(@RequestBody PaperExportRequest request) {
        String text = "";
        if (request.getParagraphs() != null && !request.getParagraphs().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (PaperExportRequest.ParagraphMapping p : request.getParagraphs()) {
                sb.append(p.getText()).append("\n\n");
            }
            text = sb.toString();
        }

        try {
            byte[] result = exportService.generateStandardDocx(text);
            String fileName = request.getFileName() != null ? request.getFileName() : "export";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + ".docx\"")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(result);
        } catch (Exception e) {
            log.error("标准导出失败", e);
            return ResponseEntity.internalServerError().body("{\"error\":\"导出失败\"}");
        }
    }
}
