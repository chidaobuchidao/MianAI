package com.mianmiantong.controller.paper;

import com.mianmiantong.service.document.TemplatePreservingExportService;
import com.mianmiantong.service.document.ParagraphProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
public class PaperUploadController {

    private final TemplatePreservingExportService exportService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String lowerName = originalName != null ? originalName.toLowerCase() : "";
            byte[] fileBytes = file.getBytes();

            List<Map<String, Object>> paraList;
            String fullText;

            if (lowerName.endsWith(".docx")) {
                List<ParagraphProfile> paragraphs = exportService.parseParagraphs(fileBytes);
                fullText = paragraphs.stream()
                    .map(ParagraphProfile::text)
                    .collect(Collectors.joining("\n\n"));
                paraList = paragraphs.stream()
                    .map(p -> Map.<String, Object>of("index", p.index(), "text", p.text()))
                    .collect(Collectors.toList());
            } else if (lowerName.endsWith(".pdf")) {
                paraList = extractPdfParagraphs(fileBytes);
                fullText = paraList.stream()
                    .map(p -> (String) p.get("text"))
                    .collect(Collectors.joining("\n\n"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件格式，请上传 .docx 或 .pdf 文件"));
            }

            String uploadId = UUID.randomUUID().toString();
            uploadCache.put(uploadId, fileBytes);

            return ResponseEntity.ok(Map.of(
                "uploadId", uploadId,
                "fullText", fullText,
                "paragraphs", paraList,
                "paragraphCount", paraList.size(),
                "fileType", lowerName.endsWith(".pdf") ? "pdf" : "docx"
            ));
        } catch (IOException e) {
            log.error("Failed to parse uploaded file", e);
            return ResponseEntity.badRequest().body(Map.of("error", "文件解析失败: " + e.getMessage()));
        }
    }

    /** Extract text from PDF and split into paragraphs */
    private List<Map<String, Object>> extractPdfParagraphs(byte[] fileBytes) throws IOException {
        List<Map<String, Object>> paragraphs = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String fullText = stripper.getText(document);

            // Split by blank lines and numbered sections
            String[] parts = fullText.split("\\n\\s*\\n");
            int index = 0;
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) continue;
                // Further split long paragraphs at numbered patterns (1. 2. or 1、2、etc.)
                String[] subParts = trimmed.split("(?=\\n(?=\\d+[.、．)）]))");
                for (String sub : subParts) {
                    String text = sub.trim().replaceAll("\\s+", " ");
                    if (text.length() > 5) {
                        paragraphs.add(Map.of("index", index++, "text", text));
                    }
                }
            }
        }
        return paragraphs;
    }

    private static final Map<String, byte[]> uploadCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > 50;
        }
    };

    public static byte[] getUploadedFile(String uploadId) {
        return uploadCache.get(uploadId);
    }
}
