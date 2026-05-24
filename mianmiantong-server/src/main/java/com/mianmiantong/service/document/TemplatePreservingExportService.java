package com.mianmiantong.service.document;

import com.mianmiantong.dto.paper.PaperExportRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于原始 .docx 模板的格式保留导出服务。
 * 采用段落 ID 标记法：逐段匹配改写结果，保留原文档格式。
 */
@Slf4j
@Service
public class TemplatePreservingExportService {

    /** 表格/页眉页脚段落索引起始偏移，避免与正文 DOCX 位置冲突 */
    private static final int TABLE_INDEX_OFFSET = 1_000_000;

    /** 从原始 DOCX 解析所有可改写段落的格式快照。
     *  正文段落使用原始 DOCX 位置作为索引（与 writeBack 对齐），
     *  表格/页眉页脚使用偏移索引避免冲突。 */
    public List<ParagraphProfile> parseParagraphs(byte[] originalDocx) {
        List<ParagraphProfile> profiles = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(originalDocx))) {
            int tableIdx = TABLE_INDEX_OFFSET;
            int bodyCount = 0, tableCount = 0, imageSkipped = 0;

            // 1. 正文段落 — 使用原始 DOCX 位置 i 作为索引
            List<XWPFParagraph> bodyParagraphs = doc.getParagraphs();
            for (int i = 0; i < bodyParagraphs.size(); i++) {
                XWPFParagraph para = bodyParagraphs.get(i);
                if (isImageOnly(para)) { imageSkipped++; continue; }
                ParagraphProfile profile = tryExtract(i, para, 4);
                if (profile != null) { profiles.add(profile); bodyCount++; }
            }

            // 2. 表格内段落 — 使用偏移索引
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph para : cell.getParagraphs()) {
                            if (isImageOnly(para)) continue;
                            ParagraphProfile profile = tryExtract(tableIdx, para, 2);
                            if (profile != null) { profiles.add(profile); tableIdx++; tableCount++; }
                        }
                    }
                }
            }

            // 3. 页眉页脚 — 使用偏移索引
            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph para : header.getParagraphs()) {
                    ParagraphProfile profile = tryExtract(tableIdx, para, 4);
                    if (profile != null) { profiles.add(profile); tableIdx++; }
                }
            }
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph para : footer.getParagraphs()) {
                    ParagraphProfile profile = tryExtract(tableIdx, para, 4);
                    if (profile != null) { profiles.add(profile); tableIdx++; }
                }
            }

            if (profiles.isEmpty()) {
                log.warn("DOCX 段落解析为空: bodyParsed={}, tableParsed={}, imageSkipped={}, totalTables={}",
                    bodyCount, tableCount, imageSkipped, doc.getTables().size());
            } else {
                log.info("DOCX 解析成功: total={}, body={}, table={}", profiles.size(), bodyCount, tableCount);
            }
        } catch (Exception e) {
            log.error("Failed to parse paragraphs from DOCX", e);
            throw new RuntimeException("Failed to parse paragraphs from DOCX", e);
        }
        return profiles;
    }

    /** 判断段落是否只有图片（没有文字内容） */
    private boolean isImageOnly(XWPFParagraph para) {
        boolean hasImage = false, hasText = false;
        for (XWPFRun run : para.getRuns()) {
            if (run.getText(0) != null && !run.getText(0).isBlank()) hasText = true;
            if (!run.getEmbeddedPictures().isEmpty()) hasImage = true;
        }
        return hasImage && !hasText;
    }

    private ParagraphProfile tryExtract(int index, XWPFParagraph para, int minLength) {
        String text = para.getText();
        if (text == null || text.isBlank()) {
            StringBuilder sb = new StringBuilder();
            for (XWPFRun run : para.getRuns()) {
                String rt = run.getText(0);
                if (rt != null) sb.append(rt);
            }
            text = sb.toString();
        }
        if (text == null || text.isBlank()) return null;
        // 清洗：合并多余空白、移除控制字符
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
        // 去除纯数字/符号段落（页码、分隔线等）
        String stripped = text.replaceAll("[\\s\\p{P}\\p{S}0-9]", "");
        if (stripped.length() < minLength) return null;
        return ParagraphProfile.from(index, para);
    }

    /** 回退全文本提取：当段落解析为空时，提取所有文本（包括表格和短文本） */
    public String extractFallbackText(byte[] docxFile) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxFile))) {
            StringBuilder sb = new StringBuilder();
            // 正文
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = extractAllText(para);
                if (!text.isBlank()) sb.append(text).append("\n\n");
            }
            // 表格 → 每行用 tab 连接，行间换行
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cellTexts = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getParagraphs().stream()
                            .map(this::extractAllText)
                            .filter(t -> !t.isBlank())
                            .collect(Collectors.joining(" "));
                        if (!cellText.isBlank()) cellTexts.add(cellText);
                    }
                    if (!cellTexts.isEmpty()) {
                        sb.append(String.join("\t", cellTexts)).append("\n");
                    }
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Fallback text extraction failed", e);
            return "";
        }
    }

    /** 从段落中提取所有文本，包括被格式分割的片段 */
    private String extractAllText(XWPFParagraph para) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t != null) sb.append(t);
        }
        String text = sb.toString();
        if (text.isBlank()) text = para.getText();
        return text != null ? text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim() : "";
    }

    /** 将改写后的段落写回原 DOCX，保留所有格式。
     * @param skipTableMatching true=跳过表格内容匹配（简历场景，避免误改） */
    public byte[] writeBack(byte[] originalDocx, Map<Integer, String> rewrittenParagraphs,
                            boolean skipTableMatching) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(originalDocx));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int matched = 0;
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (int i = 0; i < paragraphs.size(); i++) {
                if (!rewrittenParagraphs.containsKey(i)) continue;
                String newText = rewrittenParagraphs.get(i);
                replaceParagraphText(paragraphs.get(i), newText);
                matched++;
            }

            int total = rewrittenParagraphs.size();
            if (matched == 0) {
                throw new RuntimeException("段落匹配完全失败，回退到 Markdown 导出");
            }
            log.info("格式保留导出: 改写段落 {}/{} 匹配成功", matched, total);

            // 尝试替换表格内文本（简历场景跳过，因为 parseParagraphs 已将表格段落纳入索引匹配）
            if (!skipTableMatching) {
                for (XWPFTable table : doc.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                String cellText = para.getText();
                                if (cellText == null || cellText.isBlank()) continue;
                                for (Map.Entry<Integer, String> e : rewrittenParagraphs.entrySet()) {
                                    String rewrote = e.getValue();
                                    if (rewrote != null && longestCommonSubstring(cellText, rewrote) > 10) {
                                        replaceParagraphText(para, rewrote);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("格式保留导出写回失败", e);
            throw new RuntimeException("格式保留导出失败: " + e.getMessage());
        }
    }

    /** 完整导出流程：接收前端传来的段落映射，直接写回 */
    public byte[] exportWithPreservedFormat(byte[] originalDocx, PaperExportRequest request) {
        Map<Integer, String> paraMap = new LinkedHashMap<>();
        if (request.getParagraphs() != null) {
            for (PaperExportRequest.ParagraphMapping pm : request.getParagraphs()) {
                paraMap.put(pm.getIndex(), pm.getText());
            }
        }
        return writeBack(originalDocx, paraMap, false);
    }

    /** 保留原段落格式替换文本（字体/大小/粗斜体/颜色/下划线/删除线）。
     *  段落含图片时跳过，保留原内容不变。写入前自动剥离 Markdown 标记。 */
    void replaceParagraphText(XWPFParagraph para, String newText) {
        newText = stripMarkdown(newText);
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) {
            para.createRun().setText(newText);
            return;
        }

        // 包含图片的段落不修改
        for (XWPFRun r : runs) {
            if (!r.getEmbeddedPictures().isEmpty()) return;
        }

        XWPFRun templateRun = null;
        for (XWPFRun r : runs) {
            String t = r.getText(0);
            if (t != null && !t.isEmpty()) { templateRun = r; break; }
        }
        if (templateRun == null) templateRun = runs.get(0);

        String fontFamily = templateRun.getFontFamily();
        String eastAsia = templateRun.getFontFamily(XWPFRun.FontCharRange.eastAsia);
        Double fontSize = templateRun.getFontSizeAsDouble();
        if (fontSize != null && fontSize <= 0) fontSize = null;
        boolean bold = templateRun.isBold();
        boolean italic = templateRun.isItalic();
        String color = templateRun.getColor();
        UnderlinePatterns underline = templateRun.getUnderline();
        boolean strikeThrough = templateRun.isStrikeThrough();

        for (int i = runs.size() - 1; i >= 0; i--) {
            para.removeRun(i);
        }

        String[] lines = newText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            XWPFRun newRun = para.createRun();
            newRun.setText(lines[i]);
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (eastAsia != null) newRun.setFontFamily(eastAsia, XWPFRun.FontCharRange.eastAsia);
            if (fontSize != null) newRun.setFontSize(fontSize);
            newRun.setBold(bold);
            newRun.setItalic(italic);
            if (color != null) newRun.setColor(color);
            if (underline != null && underline != UnderlinePatterns.NONE) newRun.setUnderline(underline);
            newRun.setStrikeThrough(strikeThrough);
            if (i < lines.length - 1) newRun.addBreak();
        }
    }

    /** 标准导出：纯文本生成 DOCX（回退方案，不保留原格式） */
    public byte[] generateStandardDocx(String text) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String[] paragraphs = text.split("\n\n");
            for (String paraText : paragraphs) {
                String trimmed = paraText.trim();
                if (trimmed.isEmpty()) continue;
                XWPFParagraph para = doc.createParagraph();
                para.setFirstLineIndent(480);
                XWPFRun run = para.createRun();
                run.setText(trimmed);
                run.setFontFamily("Times New Roman");
                run.setFontSize(12);
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Standard DOCX generation failed", e);
            throw new RuntimeException("标准导出失败", e);
        }
    }

    private int longestCommonSubstring(String a, String b) {
        int m = a.length(), n = b.length(), max = 0;
        int[] dp = new int[n + 1];
        for (int i = 1; i <= m; i++) {
            int prev = 0;
            for (int j = 1; j <= n; j++) {
                int temp = dp[j];
                dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ? prev + 1 : 0;
                prev = temp;
                max = Math.max(max, dp[j]);
            }
        }
        return max;
    }

    // ======================== 语义匹配 ========================

    /** 规范化文本用于模糊匹配：去空白、去标点、小写 */
    private String normalizeForMatch(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase().trim();
    }

    /** 最长公共子序列（LCS）长度 */
    private int longestCommonSubsequence(String a, String b) {
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                curr[j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? prev[j - 1] + 1
                        : Math.max(prev[j], curr[j - 1]);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[n];
    }

    /** 计算两段规范化文本的相似度 (0.0 ~ 1.0) */
    private double textSimilarity(String a, String b) {
        int lcs = longestCommonSubsequence(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return maxLen > 0 ? (double) lcs / maxLen : 0;
    }

    /**
     * 在段落列表中查找与 searchText 最匹配的段落索引。
     * @param searchText 搜索文本
     * @param profiles 段落格式快照列表
     * @param threshold 相似度阈值 (0.0~1.0)
     * @param excludeIndices 排除的索引（已匹配的段落不再参与匹配）
     * @return 最佳匹配的段落索引，无匹配返回 -1
     */
    public int findBestMatch(String searchText, List<ParagraphProfile> profiles,
                              double threshold, Set<Integer> excludeIndices) {
        String normalizedSearch = normalizeForMatch(searchText);
        if (normalizedSearch.isEmpty()) return -1;

        int bestIdx = -1;
        double bestScore = 0;

        for (ParagraphProfile p : profiles) {
            if (excludeIndices != null && excludeIndices.contains(p.index())) continue;
            String normalizedProfile = normalizeForMatch(p.text());
            if (normalizedProfile.isEmpty()) continue;

            double score = textSimilarity(normalizedSearch, normalizedProfile);
            if (score > bestScore && score >= threshold) {
                bestScore = score;
                bestIdx = p.index();
            }
        }
        return bestIdx;
    }

    /**
     * 使用 AI 返回的 highlights 构建段落映射。
     * 每个 highlight 的 before 文本用于在 DOCX 中做模糊匹配，
     * 找到对应段落后映射到 after 文本。
     */
    public Map<Integer, String> buildMappingsFromHighlights(
            List<ParagraphProfile> profiles,
            List<Map<String, Object>> highlights) {
        Map<Integer, String> mappings = new LinkedHashMap<>();
        if (highlights == null || highlights.isEmpty()) return mappings;

        Set<Integer> usedIndices = new HashSet<>();

        for (Map<String, Object> h : highlights) {
            String before = (String) h.get("before");
            String after = (String) h.get("after");
            if (before == null || before.isBlank() || after == null || after.isBlank()) continue;

            int bestIdx = findBestMatch(before, profiles, 0.35, usedIndices);
            if (bestIdx >= 0) {
                mappings.put(bestIdx, after);
                usedIndices.add(bestIdx);
            }
        }

        log.info("highlights语义匹配: {}/{} 条匹配成功", mappings.size(), highlights.size());
        return mappings;
    }

    /**
     * 剥离 Markdown 格式标记，返回纯文本。
     * 处理：**加粗** *斜体* ##标题 -列表 `代码` [链接](url) ~~删除线~~
     */
    static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) return text;

        String result = text;
        // 粗体 **text** 或 __text__
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        result = result.replaceAll("__(.+?)__", "$1");
        // 斜体 *text* 或 _text_（注意避免破坏粗体已处理的）
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "$1");
        // 删除线 ~~text~~
        result = result.replaceAll("~~(.+?)~~", "$1");
        // 行内代码 `text`
        result = result.replaceAll("`(.+?)`", "$1");
        // 链接 [text](url)
        result = result.replaceAll("\\[(.+?)\\]\\([^)]*\\)", "$1");
        // 标题标记（行首）
        result = result.replaceAll("(?m)^#{1,6}\\s+", "");
        // 无序列表标记（行首）
        result = result.replaceAll("(?m)^[-*+]\\s+", "");
        // 引用标记（行首）
        result = result.replaceAll("(?m)^>\\s+", "");
        // 有序列表（行首）
        result = result.replaceAll("(?m)^\\d+\\.\\s+", "");

        return result.trim();
    }
}
