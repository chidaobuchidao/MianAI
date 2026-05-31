package com.mianmiantong.controller.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.paper.ExportErrorResponse;
import com.mianmiantong.dto.paper.PaperExportRequest;
import com.mianmiantong.service.document.DocumentAiService;
import com.mianmiantong.service.document.TemplatePreservingExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

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
    private final DocumentAiService documentAiService;
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
            if (!isDocxFile(file)) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ExportErrorResponse.of("原格式导出仅支持DOCX",
                        "当前上传文件不是有效的DOCX文档。PDF无法通过DOCX模板补丁保留原始版式和图片，请使用标准Word导出。",
                        "PDF保留版式导出需要单独的PDF导出链路。"));
            }

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
            String msg = e.getMessage() != null ? e.getMessage() : "未知错误";
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExportErrorResponse.of("导出失败", msg, "请尝试使用标准导出（不含原格式）"));
        }
    }

    @PostMapping("/pdf-to-docx-preserve-format")
    public ResponseEntity<?> exportPdfToDocxPreserveFormat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mappings") String mappingsJson) {

        try {
            if (!JwtAuthFilter.isAdmin()) {
                return ResponseEntity.status(403)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ExportErrorResponse.of(
                        "PDF转Word保留导出仅管理员可用",
                        "PDF转Word导出仍处于测试版，普通用户请上传DOCX文档以保证导出格式完整。",
                        "建议使用Word文档（.docx）进行论文润色、降AI和降查重导出。"));
            }

            if (!isPdfFile(file)) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ExportErrorResponse.of("PDF转Word导出仅支持PDF", "当前上传文件不是PDF文档。"));
            }

            byte[] pdfBytes = file.getBytes();
            PaperExportRequest request = objectMapper.readValue(mappingsJson, PaperExportRequest.class);
            byte[] convertedDocx = documentAiService.convertPdfToWord(
                new ByteArrayInputStream(pdfBytes),
                safeOriginalFileName(file, "paper.pdf")
            );
            byte[] result = exportService.exportConvertedPdfDocx(convertedDocx, request);
            String fileName = request.getFileName() != null ? request.getFileName() : "pdf-converted";

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + ".docx\"")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(result);
        } catch (Exception e) {
            log.error("PDF转Word保留导出失败", e);
            String msg = e.getMessage() != null ? e.getMessage() : "未知错误";
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExportErrorResponse.of("PDF转Word保留导出失败", msg, "请改用标准Word导出，或稍后重试PDF转Word Beta。"));
        }
    }

    private boolean isDocxFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".docx");
    }

    private boolean isPdfFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".pdf");
    }

    private String safeOriginalFileName(MultipartFile file, String fallback) {
        String fileName = file.getOriginalFilename();
        return fileName == null || fileName.isBlank() ? fallback : fileName;
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
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExportErrorResponse.of("导出失败", e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }
}
