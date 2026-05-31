package com.mianmiantong.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies text patches to a DOCX while preserving the original package.
 *
 * <p>The export path uses raw OOXML patching for every paragraph type. Apache
 * POI is still used by package-private helpers and tests, but the production
 * path avoids {@code XWPFDocument.write(...)} so Word styles, drawings, images,
 * relationships, section settings, and template XML are not rebuilt during
 * format-preserving export.</p>
 */
@Slf4j
public class DocxPatchApplier {

    private static final double BEFORE_SIMILARITY_THRESHOLD = 0.5;

    private static final Pattern BODY_P = Pattern.compile("body\\.p\\[(\\d+)]");
    private static final Pattern TABLE_CELL_P = Pattern.compile(
        "body\\.table\\[(\\d+)]\\.row\\[(\\d+)]\\.cell\\[(\\d+)]\\.p\\[(\\d+)]");
    private static final Pattern HEADER_P = Pattern.compile("header\\[(\\d+)]\\.p\\[(\\d+)]");
    private static final Pattern FOOTER_P = Pattern.compile("footer\\[(\\d+)]\\.p\\[(\\d+)]");

    private final DocxDocumentXmlPatcher documentXmlPatcher = new DocxDocumentXmlPatcher();

    private record PatchOutcome(byte[] bytes, int succeeded, int failed, List<String> failures) {}

    public byte[] applyPatches(byte[] originalDocx, List<DocxPatch> patches, Map<String, DocxPath> textBoxTextToPath) {
        if (patches == null || patches.isEmpty()) {
            return originalDocx;
        }

        DocxDocumentXmlPatcher.Result result = documentXmlPatcher.applyPatches(originalDocx, patches);
        byte[] current = result.bytes();
        int succeeded = result.succeeded();
        int failed = result.failed();
        List<String> failures = result.failures();

        if (succeeded == 0 && failed > 0) {
            throw new RuntimeException("All patches failed: " + String.join("; ", failures));
        }
        if (failed > 0) {
            log.warn("Partial patch failure: succeeded={}, failed={}, details={}", succeeded, failed, failures);
        } else {
            log.info("All {} patches applied successfully", succeeded);
        }
        return current;
    }

    public byte[] applyPatches(byte[] originalDocx, List<DocxPatch> patches) {
        return applyPatches(originalDocx, patches, Map.of());
    }

    private PatchOutcome applyPoiPatches(byte[] originalDocx, List<DocxPatch> patches) {
        int succeeded = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(originalDocx))) {
            for (DocxPatch patch : patches) {
                try {
                    String pathStr = patch.path().pathString();
                    XWPFParagraph para = resolvePath(doc, pathStr);
                    if (para == null) {
                        failed++;
                        failures.add("path not found: " + pathStr);
                        continue;
                    }
                    if (!validateBefore(para, patch.before())) {
                        failed++;
                        failures.add("before mismatch: " + pathStr);
                        continue;
                    }
                    replaceTextInRuns(para, patch.after());
                    succeeded++;
                } catch (Exception e) {
                    failed++;
                    failures.add(patch.path().pathString() + ": " + e.getMessage());
                }
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                doc.write(out);
                return new PatchOutcome(out.toByteArray(), succeeded, failed, failures);
            }
        } catch (Exception e) {
            throw new RuntimeException("POI patch application failed: " + e.getMessage(), e);
        }
    }

    XWPFParagraph resolvePath(XWPFDocument doc, String pathString) {
        Matcher m;

        m = BODY_P.matcher(pathString);
        if (m.matches()) {
            int pi = Integer.parseInt(m.group(1));
            List<XWPFParagraph> paras = doc.getParagraphs();
            return pi < paras.size() ? paras.get(pi) : null;
        }

        m = TABLE_CELL_P.matcher(pathString);
        if (m.matches()) {
            int ti = Integer.parseInt(m.group(1));
            int ri = Integer.parseInt(m.group(2));
            int ci = Integer.parseInt(m.group(3));
            int pi = Integer.parseInt(m.group(4));
            List<XWPFTable> tables = doc.getTables();
            if (ti >= tables.size()) return null;
            List<XWPFTableRow> rows = tables.get(ti).getRows();
            if (ri >= rows.size()) return null;
            List<XWPFTableCell> cells = rows.get(ri).getTableCells();
            if (ci >= cells.size()) return null;
            List<XWPFParagraph> paras = cells.get(ci).getParagraphs();
            return pi < paras.size() ? paras.get(pi) : null;
        }

        m = HEADER_P.matcher(pathString);
        if (m.matches()) {
            int hi = Integer.parseInt(m.group(1));
            int pi = Integer.parseInt(m.group(2));
            List<XWPFHeader> headers = doc.getHeaderList();
            if (hi >= headers.size()) return null;
            List<XWPFParagraph> paras = headers.get(hi).getParagraphs();
            return pi < paras.size() ? paras.get(pi) : null;
        }

        m = FOOTER_P.matcher(pathString);
        if (m.matches()) {
            int fi = Integer.parseInt(m.group(1));
            int pi = Integer.parseInt(m.group(2));
            List<XWPFFooter> footers = doc.getFooterList();
            if (fi >= footers.size()) return null;
            List<XWPFParagraph> paras = footers.get(fi).getParagraphs();
            return pi < paras.size() ? paras.get(pi) : null;
        }

        return null;
    }

    private boolean validateBefore(XWPFParagraph para, String before) {
        if (before == null || before.isBlank()) return true;
        return validateBeforeText(extractText(para), before);
    }

    private boolean validateBeforeText(String currentText, String before) {
        if (before == null || before.isBlank()) return true;
        String normalizedCurrent = DocxTextUtils.normalize(currentText);
        String normalizedBefore = DocxTextUtils.normalize(before);
        if (normalizedCurrent.isBlank() || normalizedBefore.isBlank()) return false;

        if (normalizedBefore.length() <= 4 || normalizedCurrent.length() <= 4) {
            return normalizedCurrent.contains(normalizedBefore)
                || normalizedBefore.contains(normalizedCurrent);
        }

        double threshold = normalizedBefore.length() < 15 ? 0.6 : BEFORE_SIMILARITY_THRESHOLD;
        double sim = DocxTextUtils.similarity(normalizedCurrent, normalizedBefore);
        return sim >= threshold
            || normalizedCurrent.contains(normalizedBefore)
            || normalizedBefore.contains(normalizedCurrent);
    }

    private String extractText(XWPFParagraph para) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t != null) sb.append(t);
        }
        String text = sb.toString();
        if (text.isBlank()) text = para.getText();
        return text != null ? text.trim() : "";
    }

    void replaceTextInRuns(XWPFParagraph para, String newText) {
        newText = DocxTextUtils.stripMarkdown(newText);
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) {
            para.createRun().setText(newText);
            return;
        }

        List<Integer> textRunIndices = new ArrayList<>();
        for (int i = 0; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            if (!run.getEmbeddedPictures().isEmpty()) continue;
            if (run.getText(0) != null) {
                textRunIndices.add(i);
            }
        }

        if (textRunIndices.isEmpty()) {
            para.createRun().setText(newText);
            return;
        }

        if (textRunIndices.size() == 1) {
            setRunTextWithBreaks(runs.get(textRunIndices.get(0)), newText);
            return;
        }

        int[] origLengths = new int[textRunIndices.size()];
        int totalOrigLen = 0;
        for (int i = 0; i < textRunIndices.size(); i++) {
            String t = runs.get(textRunIndices.get(i)).getText(0);
            origLengths[i] = t != null ? t.length() : 0;
            totalOrigLen += origLengths[i];
        }

        if (totalOrigLen == 0) {
            setRunTextWithBreaks(runs.get(textRunIndices.get(0)), newText);
            for (int ri = 1; ri < textRunIndices.size(); ri++) {
                runs.get(textRunIndices.get(ri)).setText("", 0);
            }
            return;
        }

        String[] chunks = new String[textRunIndices.size()];
        int pos = 0;
        for (int i = 0; i < textRunIndices.size(); i++) {
            if (i == textRunIndices.size() - 1) {
                chunks[i] = newText.substring(pos);
            } else {
                double ratio = (double) origLengths[i] / totalOrigLen;
                int end = pos + (int) Math.round(newText.length() * ratio);
                end = findSplitBoundary(newText, pos, end);
                chunks[i] = newText.substring(pos, end);
                pos = end;
            }
        }

        for (int i = 0; i < textRunIndices.size(); i++) {
            setRunTextWithBreaks(runs.get(textRunIndices.get(i)), chunks[i]);
        }
    }

    private int findSplitBoundary(String text, int start, int target) {
        if (target <= start) return start;
        if (target >= text.length()) return text.length();

        int best = target;
        int searchRange = Math.min(15, Math.max(1, (target - start) / 2));
        for (int i = 0; i <= searchRange; i++) {
            int fwd = target + i;
            if (fwd < text.length() && isGoodBoundary(text, fwd)) {
                best = fwd;
                break;
            }
            int bwd = target - i;
            if (bwd > start && isGoodBoundary(text, bwd)) {
                best = bwd;
                break;
            }
        }
        return best;
    }

    private boolean isGoodBoundary(String text, int pos) {
        if (pos <= 0 || pos >= text.length()) return false;
        char before = text.charAt(pos - 1);
        char after = text.charAt(pos);
        return before == ' '
            || after == ' '
            || before == '\n'
            || before == ';'
            || before == ','
            || before == '.'
            || before == ':';
    }

    private void setRunTextWithBreaks(XWPFRun run, String text) {
        if (text.isEmpty()) {
            run.setText("", 0);
            return;
        }

        String[] lines = text.split("\n", -1);
        run.setText(lines[0], 0);
        for (int i = 1; i < lines.length; i++) {
            run.addBreak();
            run.setText(lines[i], i);
        }
        for (int i = lines.length; i < run.getCTR().sizeOfTArray(); i++) {
            run.setText("", i);
        }
    }

    static String stripMarkdown(String text) {
        return DocxTextUtils.stripMarkdown(text);
    }
}
