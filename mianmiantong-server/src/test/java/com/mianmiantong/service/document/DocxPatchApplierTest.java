package com.mianmiantong.service.document;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocxPatchApplierTest {

    private final DocxPatchApplier applier = new DocxPatchApplier();

    // ---- helpers ----

    private byte[] toBytes(XWPFDocument doc) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.write(out);
            return out.toByteArray();
        }
    }

    private XWPFDocument reopen(byte[] bytes) throws IOException {
        return new XWPFDocument(new ByteArrayInputStream(bytes));
    }

    private Map<String, byte[]> zipEntries(byte[] bytes) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                result.put(entry.getName(), zin.readAllBytes());
                zin.closeEntry();
            }
        }
        return result;
    }

    private byte[] replaceZipEntry(byte[] bytes, String targetEntryName, byte[] replacement) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zout = new ZipOutputStream(out)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                ZipEntry outEntry = new ZipEntry(entry.getName());
                zout.putNextEntry(outEntry);
                zout.write(entry.getName().equals(targetEntryName) ? replacement : zin.readAllBytes());
                zout.closeEntry();
                zin.closeEntry();
            }
            zout.finish();
            return out.toByteArray();
        }
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    // ---- replaceTextInRuns ----

    @Nested
    @DisplayName("replaceTextInRuns")
    class ReplaceTextInRuns {

        @Test
        @DisplayName("single run: replaces text, preserves formatting")
        void singleRun() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText("old text");
            run.setBold(true);
            run.setFontSize(14);
            run.setFontFamily("Arial");

            applier.replaceTextInRuns(para, "new text");

            assertThat(para.getRuns()).hasSize(1);
            assertThat(para.getRuns().get(0).getText(0)).isEqualTo("new text");
            assertThat(para.getRuns().get(0).isBold()).isTrue();
            assertThat(para.getRuns().get(0).getFontSize()).isEqualTo(14);
            assertThat(para.getRuns().get(0).getFontFamily()).isEqualTo("Arial");
            doc.close();
        }

        @Test
        @DisplayName("multiple runs: distributes proportionally, preserves each run's formatting")
        void multipleRuns() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();

            XWPFRun run0 = para.createRun();
            run0.setText("Important term");   // 14 chars
            run0.setBold(true);

            XWPFRun run1 = para.createRun();
            run1.setText(" and more text");   // 14 chars
            run1.setItalic(true);

            applier.replaceTextInRuns(para, "完全不同的替换文本");

            // Text is distributed proportionally across runs
            String run0Text = para.getRuns().get(0).getText(0);
            String run1Text = para.getRuns().get(1).getText(0);
            assertThat(run0Text).isNotEmpty();
            assertThat(run1Text).isNotEmpty();
            // Combined text matches the full replacement
            assertThat(run0Text + run1Text).isEqualTo("完全不同的替换文本");
            // Each run preserves its original formatting
            assertThat(para.getRuns().get(0).isBold()).isTrue();
            assertThat(para.getRuns().get(1).isItalic()).isTrue();
            doc.close();
        }

        @Test
        @DisplayName("preserves image-only runs")
        void preservesImages() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();

            XWPFRun textRun = para.createRun();
            textRun.setText("hello");

            // Create a run with a drawing element (simulating an image)
            XWPFRun imageRun = para.createRun();
            imageRun.setText(""); // text at pos 0
            // Add a drawing via CTR to simulate an image
            imageRun.getCTR().addNewDrawing();

            applier.replaceTextInRuns(para, "world");

            // Text run gets new text
            assertThat(para.getRuns().get(0).getText(0)).isEqualTo("world");
            // Image run still has its drawing
            assertThat(para.getRuns().get(1).getCTR().getDrawingList()).isNotEmpty();
            doc.close();
        }

        @Test
        @DisplayName("no text runs: creates new run")
        void noTextRuns() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();

            // Run with null text (image-only simulation)
            XWPFRun imageRun = para.createRun();
            imageRun.getCTR().addNewDrawing();

            applier.replaceTextInRuns(para, "new text");

            assertThat(para.getRuns()).hasSize(2);
            assertThat(para.getRuns().get(1).getText(0)).isEqualTo("new text");
            doc.close();
        }

        @Test
        @DisplayName("empty paragraph: creates run")
        void emptyParagraph() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();

            applier.replaceTextInRuns(para, "new text");

            assertThat(para.getRuns()).hasSize(1);
            assertThat(para.getRuns().get(0).getText(0)).isEqualTo("new text");
            doc.close();
        }

        @Test
        @DisplayName("strips markdown from replacement text")
        void stripsMarkdown() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText("old");

            applier.replaceTextInRuns(para, "**bold** and *italic*");

            assertThat(para.getRuns().get(0).getText(0)).isEqualTo("bold and italic");
            doc.close();
        }
    }

    // ---- validateBefore (tested via applyPatches) ----

    @Nested
    @DisplayName("validateBefore via applyPatches")
    class ValidateBefore {

        @Test
        @DisplayName("exact match: patch applied")
        void exactMatch() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText("hello world");
            byte[] bytes = toBytes(doc);
            doc.close();

            DocxPath path = DocxPath.body(0);
            DocxPatch patch = new DocxPatch(path, "hello world", "new text");
            byte[] result = applier.applyPatches(bytes, List.of(patch));
            XWPFDocument out = reopen(result);

            assertThat(out.getParagraphs().get(0).getRuns().get(0).getText(0)).isEqualTo("new text");
            out.close();
        }

        @Test
        @DisplayName("short before text: substring containment passes")
        void shortTextSubstring() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText("AB CD EF");
            byte[] bytes = toBytes(doc);
            doc.close();

            DocxPath path = DocxPath.body(0);
            // "AB" is a substring of "AB CD EF" — snippet replacement preserves the rest
            DocxPatch patch = new DocxPatch(path, "AB", "replaced");
            byte[] result = applier.applyPatches(bytes, List.of(patch));
            XWPFDocument out = reopen(result);

            assertThat(out.getParagraphs().get(0).getText()).isEqualTo("replaced CD EF");
            out.close();
        }

        @Test
        @DisplayName("mismatched before text: patch rejected")
        void mismatch() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText("hello world");
            byte[] bytes = toBytes(doc);
            doc.close();

            DocxPath path = DocxPath.body(0);
            DocxPatch patch = new DocxPatch(path, "completely different text that does not match", "new");
            // All patches fail → RuntimeException
            assertThatThrownBy(() -> applier.applyPatches(bytes, List.of(patch)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("All patches failed");
        }

        @Test
        @DisplayName("null before: always passes")
        void nullBefore() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText("hello");
            byte[] bytes = toBytes(doc);
            doc.close();

            DocxPath path = DocxPath.body(0);
            DocxPatch patch = new DocxPatch(path, null, "replaced");
            byte[] result = applier.applyPatches(bytes, List.of(patch));
            XWPFDocument out = reopen(result);

            assertThat(out.getParagraphs().get(0).getRuns().get(0).getText(0)).isEqualTo("replaced");
            out.close();
        }

        @Test
        @DisplayName("formatting preserved through full round-trip")
        void formattingPreserved() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            para.setFirstLineIndent(480);
            XWPFRun run = para.createRun();
            run.setText("original text");
            run.setBold(true);
            run.setItalic(true);
            run.setFontSize(16);
            run.setFontFamily("SimSun");
            run.setColor("FF0000");
            byte[] bytes = toBytes(doc);
            doc.close();

            DocxPatch patch = new DocxPatch(DocxPath.body(0), "original text", "rewritten text");
            byte[] result = applier.applyPatches(bytes, List.of(patch));
            XWPFDocument out = reopen(result);

            XWPFRun outRun = out.getParagraphs().get(0).getRuns().get(0);
            assertThat(outRun.getText(0)).isEqualTo("rewritten text");
            assertThat(outRun.isBold()).isTrue();
            assertThat(outRun.isItalic()).isTrue();
            assertThat(outRun.getFontSize()).isEqualTo(16);
            assertThat(outRun.getFontFamily()).isEqualTo("SimSun");
            assertThat(outRun.getColor()).isEqualTo("FF0000");
            assertThat(out.getParagraphs().get(0).getFirstLineIndent()).isEqualTo(480);
            out.close();
        }
    }

    // ---- resolvePath ----

    @Nested
    @DisplayName("resolvePath")
    class ResolvePath {

        @Test
        @DisplayName("body paragraph")
        void bodyParagraph() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("first");
            doc.createParagraph().createRun().setText("second");

            XWPFParagraph resolved = applier.resolvePath(doc, "body.p[1]");
            assertThat(resolved).isNotNull();
            assertThat(resolved.getText()).isEqualTo("second");
            doc.close();
        }

        @Test
        @DisplayName("table cell paragraph")
        void tableCell() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("before table");
            XWPFTable table = doc.createTable(3, 4);
            table.getRow(2).getCell(3).getParagraphs().get(0).createRun().setText("cell text");

            XWPFParagraph resolved = applier.resolvePath(doc, "body.table[0].row[2].cell[3].p[0]");
            assertThat(resolved).isNotNull();
            doc.close();
        }

        @Test
        @DisplayName("nonexistent path returns null")
        void nonexistent() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("only one");

            assertThat(applier.resolvePath(doc, "body.p[5]")).isNull();
            assertThat(applier.resolvePath(doc, "body.table[0].row[0].cell[0].p[0]")).isNull();
            assertThat(applier.resolvePath(doc, "header[0].p[0]")).isNull();
            doc.close();
        }
    }

    // ---- applyPatches ----

    @Nested
    @DisplayName("applyPatches")
    class ApplyPatches {

        @Test
        @DisplayName("partial failure: returns successful patches")
        void partialFailure() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("para one");
            doc.createParagraph().createRun().setText("para two");
            doc.createParagraph().createRun().setText("para three");
            byte[] bytes = toBytes(doc);
            doc.close();

            List<DocxPatch> patches = List.of(
                new DocxPatch(DocxPath.body(0), "para one", "replaced one"),
                new DocxPatch(DocxPath.body(1), "wrong before text that does not match at all", "should fail"),
                new DocxPatch(DocxPath.body(2), "para three", "replaced three")
            );

            byte[] result = applier.applyPatches(bytes, patches);
            XWPFDocument out = reopen(result);

            assertThat(out.getParagraphs().get(0).getRuns().get(0).getText(0)).isEqualTo("replaced one");
            assertThat(out.getParagraphs().get(1).getRuns().get(0).getText(0)).isEqualTo("para two"); // unchanged
            assertThat(out.getParagraphs().get(2).getRuns().get(0).getText(0)).isEqualTo("replaced three");
            out.close();
        }

        @Test
        @DisplayName("duplicate text: path decides which paragraph is patched")
        void duplicateTextUsesPath() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("same text");
            doc.createParagraph().createRun().setText("same text");
            byte[] bytes = toBytes(doc);
            doc.close();

            DocxPatch patch = new DocxPatch(DocxPath.body(1), "same text", "second paragraph only");
            byte[] result = applier.applyPatches(bytes, List.of(patch));
            XWPFDocument out = reopen(result);

            assertThat(out.getParagraphs().get(0).getText()).isEqualTo("same text");
            assertThat(out.getParagraphs().get(1).getText()).isEqualTo("second paragraph only");
            out.close();
        }

        @Test
        @DisplayName("raw XML patch keeps non-target package entries byte-identical")
        void nonTargetDocxEntriesStayUnchanged() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph para = doc.createParagraph();
            para.setStyle("Normal");
            XWPFRun run = para.createRun();
            run.setText("original text");
            run.setBold(true);
            byte[] bytes = toBytes(doc);
            doc.close();

            Map<String, byte[]> beforeEntries = zipEntries(bytes);
            byte[] result = applier.applyPatches(bytes, List.of(
                new DocxPatch(DocxPath.body(0), "original text", "rewritten text")));
            Map<String, byte[]> afterEntries = zipEntries(result);

            assertThat(afterEntries.keySet()).isEqualTo(beforeEntries.keySet());
            for (String entryName : beforeEntries.keySet()) {
                if ("word/document.xml".equals(entryName)) continue;
                assertThat(afterEntries.get(entryName))
                    .as("entry should remain unchanged: %s", entryName)
                    .isEqualTo(beforeEntries.get(entryName));
            }
        }

        @Test
        @DisplayName("all fail: throws exception")
        void allFail() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("hello");
            byte[] bytes = toBytes(doc);
            doc.close();

            List<DocxPatch> patches = List.of(
                new DocxPatch(DocxPath.body(0), "completely wrong", "new")
            );

            assertThatThrownBy(() -> applier.applyPatches(bytes, patches))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("All patches failed");
        }

        @Test
        @DisplayName("empty patches: returns original")
        void emptyPatches() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("hello");
            byte[] bytes = toBytes(doc);
            doc.close();

            byte[] result = applier.applyPatches(bytes, List.of());
            assertThat(result).isEqualTo(bytes);
        }

        @Test
        @DisplayName("txbx path: patches document.xml directly")
        void textBoxPathUsesDocumentXmlPatch() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("normal body");
            byte[] bytes = toBytes(doc);
            doc.close();

            String documentXml = new String(zipEntries(bytes).get("word/document.xml"), StandardCharsets.UTF_8);
            String textBoxXml = """
                <w:p><w:r><w:txbxContent>
                  <w:p><w:r><w:t>text from complex shape</w:t></w:r></w:p>
                </w:txbxContent></w:r></w:p>
                """;
            byte[] withTextBox = replaceZipEntry(
                bytes,
                "word/document.xml",
                documentXml.replace("</w:body>", textBoxXml + "</w:body>").getBytes(StandardCharsets.UTF_8));

            DocxPatch patch = new DocxPatch(
                DocxPath.textBox(0),
                "text from complex shape",
                "rewritten shape text");

            byte[] result = applier.applyPatches(withTextBox, List.of(patch));
            String patchedXml = new String(zipEntries(result).get("word/document.xml"), StandardCharsets.UTF_8);

            assertThat(patchedXml).contains("normal body");
            assertThat(patchedXml).contains("rewritten shape text");
            assertThat(patchedXml).doesNotContain("text from complex shape");
        }

        @Test
        @DisplayName("txbx path: preserves multiple run formatting")
        void textBoxPathKeepsMultipleRuns() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("normal body");
            byte[] bytes = toBytes(doc);
            doc.close();

            String documentXml = new String(zipEntries(bytes).get("word/document.xml"), StandardCharsets.UTF_8);
            String textBoxXml = """
                <w:p><w:r><w:txbxContent>
                  <w:p>
                    <w:r><w:rPr><w:b/></w:rPr><w:t>primary text </w:t></w:r>
                    <w:r><w:rPr><w:i/></w:rPr><w:t>secondary text</w:t></w:r>
                  </w:p>
                </w:txbxContent></w:r></w:p>
                """;
            byte[] withTextBox = replaceZipEntry(
                bytes,
                "word/document.xml",
                documentXml.replace("</w:body>", textBoxXml + "</w:body>").getBytes(StandardCharsets.UTF_8));

            DocxPatch patch = new DocxPatch(
                DocxPath.textBox(0),
                "primary text secondary text",
                "rewritten primary and secondary content");

            byte[] result = applier.applyPatches(withTextBox, List.of(patch));
            String patchedXml = new String(zipEntries(result).get("word/document.xml"), StandardCharsets.UTF_8);

            assertThat(patchedXml).contains("rewritten primary");
            assertThat(patchedXml).contains("secondary content");
            assertThat(patchedXml).contains("<w:b/>");
            assertThat(patchedXml).contains("<w:i/>");
            assertThat(patchedXml).doesNotContain("primary text secondary text");
        }

        @Test
        @DisplayName("txbx path: ignores blank text-box paragraphs when numbering")
        void textBoxPathIgnoresBlankParagraphsWhenNumbering() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("normal body");
            byte[] bytes = toBytes(doc);
            doc.close();

            String documentXml = new String(zipEntries(bytes).get("word/document.xml"), StandardCharsets.UTF_8);
            String textBoxXml = """
                <w:p><w:r><w:txbxContent>
                  <w:p><w:r><w:t> </w:t></w:r></w:p>
                  <w:p><w:r><w:t>target text box paragraph</w:t></w:r></w:p>
                </w:txbxContent></w:r></w:p>
                """;
            byte[] withTextBox = replaceZipEntry(
                bytes,
                "word/document.xml",
                documentXml.replace("</w:body>", textBoxXml + "</w:body>").getBytes(StandardCharsets.UTF_8));

            byte[] result = applier.applyPatches(withTextBox, List.of(
                new DocxPatch(DocxPath.textBox(0), "target text box paragraph", "patched text box paragraph")));
            String patchedXml = new String(zipEntries(result).get("word/document.xml"), StandardCharsets.UTF_8);

            assertThat(patchedXml).contains("patched text box paragraph");
            assertThat(patchedXml).doesNotContain("target text box paragraph");
        }

        @Test
        @DisplayName("txbx path: fallback never patches normal body paragraphs")
        void textBoxFallbackDoesNotPatchBodyParagraphs() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("normal body");
            byte[] bytes = toBytes(doc);
            doc.close();

            assertThatThrownBy(() -> applier.applyPatches(bytes, List.of(
                new DocxPatch(DocxPath.textBox(99), "normal body", "patched body"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("All patches failed");
        }

        @Test
        @DisplayName("txbx path: path drift falls back to text and patches compatibility duplicates")
        void textBoxPathDriftPatchesCompatibilityDuplicates() throws IOException {
            XWPFDocument doc = new XWPFDocument();
            doc.createParagraph().createRun().setText("normal body");
            byte[] bytes = toBytes(doc);
            doc.close();

            String documentXml = new String(zipEntries(bytes).get("word/document.xml"), StandardCharsets.UTF_8);
            String textBoxXml = """
                <w:p><w:r><w:txbxContent>
                  <w:p><w:r><w:t>course line original</w:t></w:r></w:p>
                </w:txbxContent></w:r></w:p>
                <w:p><w:r><w:txbxContent>
                  <w:p><w:r><w:t>course line original</w:t></w:r></w:p>
                </w:txbxContent></w:r></w:p>
                """;
            byte[] withTextBox = replaceZipEntry(
                bytes,
                "word/document.xml",
                documentXml.replace("</w:body>", textBoxXml + "</w:body>").getBytes(StandardCharsets.UTF_8));

            byte[] result = applier.applyPatches(withTextBox, List.of(
                new DocxPatch(DocxPath.textBox(12), "course line original", "course line patched")));
            String patchedXml = new String(zipEntries(result).get("word/document.xml"), StandardCharsets.UTF_8);

            assertThat(patchedXml).doesNotContain("course line original");
            assertThat(countOccurrences(patchedXml, "course line patched")).isEqualTo(2);
        }
    }
}
