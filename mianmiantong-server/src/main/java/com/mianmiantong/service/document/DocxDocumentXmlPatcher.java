package com.mianmiantong.service.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Applies patches to raw word/document.xml for paragraphs that POI cannot
 * address reliably, such as text inside shapes and text boxes.
 */
class DocxDocumentXmlPatcher {

    private static final double BEFORE_SIMILARITY_THRESHOLD = 0.5;
    private static final Pattern HEADER_PATH = Pattern.compile("header\\[(\\d+)]\\.p\\[(\\d+)]");
    private static final Pattern FOOTER_PATH = Pattern.compile("footer\\[(\\d+)]\\.p\\[(\\d+)]");

    record Result(byte[] bytes, int succeeded, int failed, List<String> failures) {}

    private record XmlPatchResult(byte[] bytes, int succeeded, int failed, List<String> failures) {}
    private record ParagraphSlice(int start, int end, String xml, String text, String pathString) {}
    private record TextNodeSlice(int contentStart, int contentEnd, String text) {}

    private static class ParagraphFrame {
        private final int start;
        private final String pathString;
        private final boolean deferredTextBoxPath;
        private int childParagraphs;

        private ParagraphFrame(int start, String pathString) {
            this(start, pathString, false);
        }

        private ParagraphFrame(int start, String pathString, boolean deferredTextBoxPath) {
            this.start = start;
            this.pathString = pathString;
            this.deferredTextBoxPath = deferredTextBoxPath;
        }
    }

    private static class TableFrame {
        private final int index;
        private int nextRowIndex;

        private TableFrame(int index) {
            this.index = index;
        }
    }

    private static class RowFrame {
        private final int index;
        private int nextCellIndex;

        private RowFrame(int index) {
            this.index = index;
        }
    }

    private static class CellFrame {
        private final int index;
        private int nextParagraphIndex;

        private CellFrame(int index) {
            this.index = index;
        }
    }

    Result applyPatches(byte[] originalDocx, List<DocxPatch> patches) {
        int succeeded = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();
        Map<String, List<DocxPatch>> patchesByEntry = groupPatchesByEntry(patches);
        Set<String> seenPatchEntries = new LinkedHashSet<>();

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(originalDocx));
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zout = new ZipOutputStream(bos)) {

            ZipEntry entry;
            Set<String> writtenEntries = new LinkedHashSet<>();

            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                if (!writtenEntries.add(name)) {
                    continue;
                }

                ZipEntry outEntry = new ZipEntry(name);
                if (entry.getTime() >= 0) {
                    outEntry.setTime(entry.getTime());
                }
                zout.putNextEntry(outEntry);

                byte[] entryBytes = zin.readAllBytes();
                List<DocxPatch> entryPatches = patchesByEntry.get(name);
                if (entryPatches != null && !entryPatches.isEmpty()) {
                    seenPatchEntries.add(name);
                    XmlPatchResult result = patchDocumentXml(entryBytes, entryPatches);
                    entryBytes = result.bytes();
                    succeeded += result.succeeded();
                    failed += result.failed();
                    failures.addAll(result.failures());
                }

                zout.write(entryBytes);
                zout.closeEntry();
                zin.closeEntry();
            }

            for (Map.Entry<String, List<DocxPatch>> patchEntry : patchesByEntry.entrySet()) {
                if (seenPatchEntries.contains(patchEntry.getKey())) continue;
                failed += patchEntry.getValue().size();
                failures.add(patchEntry.getKey() + " not found");
            }

            zout.finish();
            return new Result(bos.toByteArray(), succeeded, failed, failures);
        } catch (Exception e) {
            throw new RuntimeException("XML patch application failed: " + e.getMessage(), e);
        }
    }

    private Map<String, List<DocxPatch>> groupPatchesByEntry(List<DocxPatch> patches) {
        Map<String, List<DocxPatch>> result = new LinkedHashMap<>();
        for (DocxPatch patch : patches) {
            if (patch == null || patch.path() == null || patch.path().pathString() == null) continue;
            String entryName = targetEntryName(patch.path().pathString());
            result.computeIfAbsent(entryName, ignored -> new ArrayList<>()).add(patch);
        }
        return result;
    }

    private String targetEntryName(String pathString) {
        Matcher headerMatcher = HEADER_PATH.matcher(pathString);
        if (headerMatcher.matches()) {
            int headerIndex = Integer.parseInt(headerMatcher.group(1));
            return "word/header" + (headerIndex + 1) + ".xml";
        }

        Matcher footerMatcher = FOOTER_PATH.matcher(pathString);
        if (footerMatcher.matches()) {
            int footerIndex = Integer.parseInt(footerMatcher.group(1));
            return "word/footer" + (footerIndex + 1) + ".xml";
        }

        return "word/document.xml";
    }

    private XmlPatchResult patchDocumentXml(byte[] documentXml, List<DocxPatch> patches) {
        String xml = new String(documentXml, StandardCharsets.UTF_8);
        int succeeded = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (DocxPatch patch : patches) {
            List<ParagraphSlice> paragraphs = collectLeafParagraphSlices(xml);
            if (isTextBoxPatch(patch)) {
                TextBoxPatchResult result = applyTextBoxPatch(xml, paragraphs, patch);
                if (!result.applied()) {
                    failed++;
                    failures.add("xml paragraph not found: " + patch.path().pathString());
                    continue;
                }
                xml = result.xml();
                succeeded++;
                continue;
            }

            ParagraphSlice target = findBestParagraphSlice(paragraphs, patch);
            if (target == null) {
                failed++;
                failures.add("xml paragraph not found: " + patch.path().pathString());
                continue;
            }

            String patchedParagraph = replaceParagraphXml(target, patch.before(), patch.after());
            if (patchedParagraph == null) {
                failed++;
                failures.add("xml paragraph has no text nodes: " + patch.path().pathString());
                continue;
            }
            xml = xml.substring(0, target.start()) + patchedParagraph + xml.substring(target.end());
            succeeded++;
        }

        return new XmlPatchResult(xml.getBytes(StandardCharsets.UTF_8), succeeded, failed, failures);
    }

    private record TextBoxPatchResult(String xml, boolean applied) {}

    private boolean isTextBoxPatch(DocxPatch patch) {
        String pathString = patch.path() == null ? null : patch.path().pathString();
        return pathString != null && pathString.startsWith("txbx[");
    }

    private TextBoxPatchResult applyTextBoxPatch(
            String xml,
            List<ParagraphSlice> paragraphs,
            DocxPatch patch) {

        List<ParagraphSlice> targets = findTextBoxParagraphSlices(paragraphs, patch);
        if (targets.isEmpty()) {
            return new TextBoxPatchResult(xml, false);
        }

        String patchedXml = xml;
        int appliedCount = 0;
        for (int i = targets.size() - 1; i >= 0; i--) {
            ParagraphSlice target = targets.get(i);
            String patchedParagraph = replaceParagraphXml(target, patch.before(), patch.after());
            if (patchedParagraph == null) continue;
            patchedXml = patchedXml.substring(0, target.start())
                + patchedParagraph
                + patchedXml.substring(target.end());
            appliedCount++;
        }

        return new TextBoxPatchResult(patchedXml, appliedCount > 0);
    }

    private List<ParagraphSlice> findTextBoxParagraphSlices(List<ParagraphSlice> paragraphs, DocxPatch patch) {
        String pathString = patch.path() == null ? null : patch.path().pathString();
        List<ParagraphSlice> pathMatches = new ArrayList<>();
        if (pathString != null) {
            for (ParagraphSlice paragraph : paragraphs) {
                if (!pathString.equals(paragraph.pathString())) continue;
                if (validateBeforeText(paragraph.text(), patch.before())) {
                    pathMatches.add(paragraph);
                }
            }
        }

        if (!pathMatches.isEmpty()) {
            String normalizedTarget = DocxTextUtils.normalize(pathMatches.get(0).text());
            List<ParagraphSlice> duplicates = new ArrayList<>();
            for (ParagraphSlice paragraph : paragraphs) {
                if (isTextBoxParagraph(paragraph)
                    && normalizedTarget.equals(DocxTextUtils.normalize(paragraph.text()))
                    && validateBeforeText(paragraph.text(), patch.before())) {
                    duplicates.add(paragraph);
                }
            }
            return duplicates;
        }

        List<ParagraphSlice> fallbackMatches = new ArrayList<>();
        for (ParagraphSlice paragraph : paragraphs) {
            if (isTextBoxParagraph(paragraph)
                && isStrongTextBoxFallbackMatch(paragraph.text(), patch.before())) {
                fallbackMatches.add(paragraph);
            }
        }
        return fallbackMatches;
    }

    private boolean isTextBoxParagraph(ParagraphSlice paragraph) {
        return paragraph.pathString() != null && paragraph.pathString().startsWith("txbx[");
    }

    private boolean isStrongTextBoxFallbackMatch(String currentText, String before) {
        if (before == null || before.isBlank()) return false;
        String normalizedCurrent = DocxTextUtils.normalize(currentText);
        String normalizedBefore = DocxTextUtils.normalize(before);
        if (normalizedCurrent.isBlank() || normalizedBefore.isBlank()) return false;

        return currentText.equals(before)
            || currentText.contains(before)
            || normalizedCurrent.equals(normalizedBefore)
            || (normalizedBefore.length() >= 10 && normalizedCurrent.contains(normalizedBefore));
    }

    private List<ParagraphSlice> collectLeafParagraphSlices(String xml) {
        List<ParagraphSlice> result = new ArrayList<>();
        List<ParagraphFrame> stack = new ArrayList<>();
        List<TableFrame> tableStack = new ArrayList<>();
        List<RowFrame> rowStack = new ArrayList<>();
        List<CellFrame> cellStack = new ArrayList<>();
        int bodyParagraphIndex = 0;
        int bodyTableIndex = 0;
        int textBoxDepth = 0;
        Set<String> existingNonTextBoxTexts = new LinkedHashSet<>();

        // Deferred text box paragraphs: collected in the first pass, assigned paths
        // in the second pass so that the dedup set matches DocxTextLocator's behavior.
        record DeferredTextBox(int start, int end, String xml, String text) {}
        List<DeferredTextBox> deferredTextBoxes = new ArrayList<>();

        Pattern tagPattern = Pattern.compile("<(/?)([A-Za-z0-9]+):(p|tbl|tr|tc|txbxContent)(?:\\s[^>]*)?>");
        Matcher matcher = tagPattern.matcher(xml);

        while (matcher.find()) {
            boolean closing = "/".equals(matcher.group(1));
            String localName = matcher.group(3);

            if (!closing) {
                switch (localName) {
                    case "txbxContent" -> textBoxDepth++;
                    case "tbl" -> {
                        int tableIndex = tableStack.isEmpty() && textBoxDepth == 0 ? bodyTableIndex++ : -1;
                        tableStack.add(new TableFrame(tableIndex));
                    }
                    case "tr" -> {
                        int rowIndex = tableStack.isEmpty() ? -1 : tableStack.get(tableStack.size() - 1).nextRowIndex++;
                        rowStack.add(new RowFrame(rowIndex));
                    }
                    case "tc" -> {
                        int cellIndex = rowStack.isEmpty() ? -1 : rowStack.get(rowStack.size() - 1).nextCellIndex++;
                        cellStack.add(new CellFrame(cellIndex));
                    }
                    case "p" -> {
                        if (!stack.isEmpty()) {
                            stack.get(stack.size() - 1).childParagraphs++;
                        }
                        String pathString;
                        boolean deferredTextBoxPath = false;
                        if (textBoxDepth > 0) {
                            pathString = null;
                            deferredTextBoxPath = true;
                        } else if (!tableStack.isEmpty() && !rowStack.isEmpty() && !cellStack.isEmpty()) {
                            TableFrame table = tableStack.get(tableStack.size() - 1);
                            RowFrame row = rowStack.get(rowStack.size() - 1);
                            CellFrame cell = cellStack.get(cellStack.size() - 1);
                            int paragraphIndex = cell.nextParagraphIndex++;
                            pathString = table.index >= 0 && row.index >= 0 && cell.index >= 0
                                ? DocxPath.tableCell(table.index, row.index, cell.index, paragraphIndex).pathString()
                                : null;
                        } else {
                            pathString = DocxPath.body(bodyParagraphIndex++).pathString();
                        }
                        stack.add(new ParagraphFrame(matcher.start(), pathString, deferredTextBoxPath));
                    }
                    default -> {
                        // No-op for future local names.
                    }
                }
                continue;
            }

            switch (localName) {
                case "txbxContent" -> textBoxDepth = Math.max(0, textBoxDepth - 1);
                case "tbl" -> {
                    if (!tableStack.isEmpty()) tableStack.remove(tableStack.size() - 1);
                }
                case "tr" -> {
                    if (!rowStack.isEmpty()) rowStack.remove(rowStack.size() - 1);
                }
                case "tc" -> {
                    if (!cellStack.isEmpty()) cellStack.remove(cellStack.size() - 1);
                }
                case "p" -> {
                    if (stack.isEmpty()) continue;
                    ParagraphFrame frame = stack.remove(stack.size() - 1);
                    if (frame.childParagraphs > 0) continue;

                    int end = matcher.end();
                    String paragraphXml = xml.substring(frame.start, end);
                    String text = extractParagraphText(paragraphXml);
                    if (!text.isBlank()) {
                        String normalized = DocxTextUtils.normalize(text);
                        if (frame.deferredTextBoxPath) {
                            // Defer text box processing: collect raw data, assign paths later
                            if (!normalized.isBlank() && countSignificantChars(text) >= 2) {
                                deferredTextBoxes.add(new DeferredTextBox(frame.start, end, paragraphXml, text));
                            }
                        } else {
                            if (!normalized.isBlank()) {
                                existingNonTextBoxTexts.add(normalized);
                            }
                            result.add(new ParagraphSlice(frame.start, end, paragraphXml, text, frame.pathString));
                        }
                    }
                }
                default -> {
                    // No-op for future local names.
                }
            }
        }

        // Second pass: assign text box paths with the complete dedup set,
        // matching DocxTextLocator's behavior (which processes body/table/hf first).
        int textBoxParagraphIndex = 0;
        Map<String, String> textBoxPathByNormalizedText = new LinkedHashMap<>();
        for (DeferredTextBox dtb : deferredTextBoxes) {
            String normalized = DocxTextUtils.normalize(dtb.text());
            if (existingNonTextBoxTexts.contains(normalized)) continue;
            String pathString = textBoxPathByNormalizedText.get(normalized);
            if (pathString == null) {
                pathString = DocxPath.textBox(textBoxParagraphIndex++).pathString();
                textBoxPathByNormalizedText.put(normalized, pathString);
            }
            result.add(new ParagraphSlice(dtb.start(), dtb.end(), dtb.xml(), dtb.text(), pathString));
        }

        return result;
    }

    private int countSignificantChars(String text) {
        String stripped = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
            .replaceAll("[\\s\\p{P}\\p{S}0-9]", "");
        return stripped.length();
    }

    private ParagraphSlice findBestParagraphSlice(List<ParagraphSlice> paragraphs, DocxPatch patch) {
        String pathString = patch.path() == null ? null : patch.path().pathString();
        if (pathString != null) {
            boolean pathFound = false;
            for (ParagraphSlice paragraph : paragraphs) {
                if (!pathString.equals(paragraph.pathString())) continue;
                pathFound = true;
                if (validateBeforeText(paragraph.text(), patch.before())) return paragraph;
            }
            if (pathFound) return null;
        }

        String before = patch.before();
        if (before == null || before.isBlank()) {
            return paragraphs.isEmpty() ? null : paragraphs.get(0);
        }

        ParagraphSlice best = null;
        double bestScore = 0;
        String normalizedBefore = DocxTextUtils.normalize(before);

        for (ParagraphSlice paragraph : paragraphs) {
            String text = paragraph.text();
            if (text.equals(before)) return paragraph;
            if (before != null && text.contains(before)) return paragraph;
            if (!validateBeforeText(text, before)) continue;

            String normalizedText = DocxTextUtils.normalize(text);
            double score = normalizedText.equals(normalizedBefore)
                ? 1.0
                : DocxTextUtils.similarity(normalizedText, normalizedBefore);
            if (score > bestScore) {
                bestScore = score;
                best = paragraph;
            }
        }
        return best;
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

    private String replaceParagraphXml(ParagraphSlice paragraph, String before, String replacement) {
        List<TextNodeSlice> textNodes = collectTextNodeSlices(paragraph.xml());
        if (textNodes.isEmpty()) return null;

        String currentText = paragraph.text();
        String cleanReplacement = DocxTextUtils.stripMarkdown(replacement);

        // Build both concatenated (no-space) and POI-style (with-space) text representations
        // to handle the mismatch between raw XML node concatenation and POI's getText().
        String concatText = buildConcatText(textNodes);
        String spacedText = buildSpacedText(textNodes);

        int spanStart = -1;
        int spanEnd = 0;
        if (before != null && !before.isBlank()) {
            // Try POI-style text first (with spaces at run boundaries)
            spanStart = spacedText.indexOf(before);
            if (spanStart >= 0) {
                // Map from spacedText positions back to concatText positions
                int[] spacedToConcat = buildSpacedToConcatMap(textNodes);
                spanEnd = spanStart + before.length();
                spanStart = spacedToConcat[spanStart];
                spanEnd = spacedToConcat[Math.min(spanEnd, spacedToConcat.length - 1)];
            } else {
                // Fallback: try concatenated text directly
                spanStart = concatText.indexOf(before);
                if (spanStart >= 0) {
                    spanEnd = spanStart + before.length();
                }
            }
        }

        if (spanStart < 0) {
            // before text not found — replace entire paragraph
            spanStart = 0;
            spanEnd = concatText.length();
        }

        List<TextNodeSlice> affectedNodes = affectedTextNodes(textNodes, spanStart, spanEnd);
        if (affectedNodes.isEmpty()) return null;
        List<String> parts = splitReplacementAcrossNodes(cleanReplacement, affectedNodes, textNodes, spanStart, spanEnd);

        StringBuilder paragraphXml = new StringBuilder(paragraph.xml());
        for (int i = affectedNodes.size() - 1; i >= 0; i--) {
            TextNodeSlice node = affectedNodes.get(i);
            int nodeStartInText = textPositionBefore(textNodes, node);
            int nodeEndInText = nodeStartInText + node.text().length();
            int overlapStart = Math.max(spanStart, nodeStartInText);
            int overlapEnd = Math.min(spanEnd, nodeEndInText);

            String prefix = node.text().substring(0, Math.max(0, overlapStart - nodeStartInText));
            String suffix = node.text().substring(Math.max(0, overlapEnd - nodeStartInText));
            String newText = parts.get(i);
            if (i == 0) newText = prefix + newText;
            if (i == affectedNodes.size() - 1) newText = newText + suffix;

            paragraphXml.replace(node.contentStart(), node.contentEnd(), escapeXmlText(newText));
        }

        return paragraphXml.toString();
    }

    /** Concatenates all text node contents without adding spaces (raw XML text). */
    private String buildConcatText(List<TextNodeSlice> textNodes) {
        StringBuilder sb = new StringBuilder();
        for (TextNodeSlice node : textNodes) {
            sb.append(node.text());
        }
        return sb.toString();
    }

    /**
     * Builds text with spaces at run boundaries, matching POI's getText() behavior.
     * When the next text node doesn't start with whitespace and the previous didn't
     * end with whitespace, inserts a space to simulate inter-run word separation.
     */
    private String buildSpacedText(List<TextNodeSlice> textNodes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < textNodes.size(); i++) {
            String t = textNodes.get(i).text();
            if (i > 0 && !t.isEmpty() && sb.length() > 0) {
                char lastChar = sb.charAt(sb.length() - 1);
                char firstChar = t.charAt(0);
                // Insert space if neither side has whitespace (matching POI inter-run behavior)
                if (!Character.isWhitespace(lastChar) && !Character.isWhitespace(firstChar)) {
                    sb.append(' ');
                }
            }
            sb.append(t);
        }
        return sb.toString();
    }

    /**
     * Builds a position mapping from spacedText indices to concatText indices.
     * spacedToConcat[spacedPos] = concatPos
     */
    private int[] buildSpacedToConcatMap(List<TextNodeSlice> textNodes) {
        int spacedLen = 0;
        for (int i = 0; i < textNodes.size(); i++) {
            String t = textNodes.get(i).text();
            if (i > 0 && !t.isEmpty()) {
                // Check if a space was inserted
                String prevText = textNodes.get(i - 1).text();
                if (!prevText.isEmpty() && !Character.isWhitespace(prevText.charAt(prevText.length() - 1))
                    && !Character.isWhitespace(t.charAt(0))) {
                    spacedLen++; // the inserted space
                }
            }
            spacedLen += t.length();
        }

        int[] map = new int[spacedLen + 1];
        int concatPos = 0;
        int spacedPos = 0;
        for (int i = 0; i < textNodes.size(); i++) {
            String t = textNodes.get(i).text();
            if (i > 0 && !t.isEmpty()) {
                String prevText = textNodes.get(i - 1).text();
                if (!prevText.isEmpty() && !Character.isWhitespace(prevText.charAt(prevText.length() - 1))
                    && !Character.isWhitespace(t.charAt(0))) {
                    // Inserted space maps to the current concat position
                    map[spacedPos] = concatPos;
                    spacedPos++;
                }
            }
            for (int j = 0; j < t.length(); j++) {
                map[spacedPos] = concatPos;
                spacedPos++;
                concatPos++;
            }
        }
        map[spacedPos] = concatPos; // sentinel
        return map;
    }

    private List<TextNodeSlice> affectedTextNodes(List<TextNodeSlice> textNodes, int spanStart, int spanEnd) {
        List<TextNodeSlice> affected = new ArrayList<>();
        int pos = 0;
        for (TextNodeSlice node : textNodes) {
            int nodeStart = pos;
            int nodeEnd = pos + node.text().length();
            if (nodeEnd > spanStart && nodeStart < spanEnd) {
                affected.add(node);
            }
            pos = nodeEnd;
        }
        return affected;
    }

    private int textPositionBefore(List<TextNodeSlice> textNodes, TextNodeSlice target) {
        int pos = 0;
        for (TextNodeSlice node : textNodes) {
            if (node == target) return pos;
            pos += node.text().length();
        }
        return pos;
    }

    private String extractParagraphText(String paragraphXml) {
        StringBuilder sb = new StringBuilder();
        for (TextNodeSlice node : collectTextNodeSlices(paragraphXml)) {
            sb.append(node.text());
        }
        return sb.toString();
    }

    private List<TextNodeSlice> collectTextNodeSlices(String paragraphXml) {
        List<TextNodeSlice> result = new ArrayList<>();
        Pattern textPattern = Pattern.compile("<([A-Za-z0-9]+):t\\b[^>]*>(.*?)</\\1:t>", Pattern.DOTALL);
        Matcher matcher = textPattern.matcher(paragraphXml);
        while (matcher.find()) {
            result.add(new TextNodeSlice(
                matcher.start(2),
                matcher.end(2),
                unescapeXmlText(matcher.group(2))));
        }
        return result;
    }

    private List<String> splitReplacementAcrossNodes(String replacement, List<TextNodeSlice> affectedNodes,
                                                       List<TextNodeSlice> allTextNodes,
                                                       int spanStart, int spanEnd) {
        List<String> chunks = new ArrayList<>();
        if (affectedNodes.size() == 1) {
            chunks.add(replacement);
            return chunks;
        }

        // Calculate the total original text length within the affected span
        int totalAffectedLength = 0;
        for (TextNodeSlice node : affectedNodes) {
            int nodeStart = textPositionBefore(allTextNodes, node);
            int nodeEnd = nodeStart + node.text().length();
            int overlapStart = Math.max(spanStart, nodeStart);
            int overlapEnd = Math.min(spanEnd, nodeEnd);
            totalAffectedLength += Math.max(0, overlapEnd - overlapStart);
        }

        if (totalAffectedLength == 0) {
            chunks.add(replacement);
            for (int i = 1; i < affectedNodes.size(); i++) chunks.add("");
            return chunks;
        }

        int pos = 0;
        for (int i = 0; i < affectedNodes.size(); i++) {
            if (i == affectedNodes.size() - 1) {
                chunks.add(replacement.substring(pos));
                break;
            }

            TextNodeSlice node = affectedNodes.get(i);
            int nodeStart = textPositionBefore(allTextNodes, node);
            int nodeEnd = nodeStart + node.text().length();
            int overlapStart = Math.max(spanStart, nodeStart);
            int overlapEnd = Math.min(spanEnd, nodeEnd);
            int nodeAffectedLength = Math.max(0, overlapEnd - overlapStart);

            double ratio = (double) nodeAffectedLength / totalAffectedLength;
            int targetEnd = pos + (int) Math.round(replacement.length() * ratio);
            int end = findSplitBoundary(replacement, pos, targetEnd);
            chunks.add(replacement.substring(pos, end));
            pos = end;
        }
        return chunks;
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

    private String escapeXmlText(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private String unescapeXmlText(String text) {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&");
    }
}
