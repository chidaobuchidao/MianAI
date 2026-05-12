package com.mianmiantong.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 基于原始 .docx 模板的导出服务
 * 保留原始文档的所有格式（字体、颜色、对齐、间距等），只替换文本内容
 */
@Slf4j
@Service
public class TemplatePreservingExportService {

    /**
     * 加载原始 .docx，用 highlights 中的 before→after 替换文本，保留所有格式
     */
    public byte[] exportWithHighlights(byte[] originalDocx, List<Map<String, Object>> highlights) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(originalDocx));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (Map<String, Object> hl : highlights) {
                String before = (String) hl.get("before");
                String after = (String) hl.get("after");
                if (before == null || after == null || before.equals(after)) continue;

                // 替换段落文本
                for (XWPFParagraph para : doc.getParagraphs()) {
                    String paraText = para.getText();
                    if (paraText == null || paraText.isBlank()) continue;
                    if (fuzzyMatch(paraText, before)) {
                        replaceParagraphText(para, after);
                    }
                }

                // 替换表格中的文本
                for (XWPFTable table : doc.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                String cellText = para.getText();
                                if (cellText == null || cellText.isBlank()) continue;
                                if (fuzzyMatch(cellText, before)) {
                                    replaceParagraphText(para, after);
                                }
                            }
                        }
                    }
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("模板保留导出失败", e);
            throw new RuntimeException("模板导出失败: " + e.getMessage());
        }
    }

    /** 保留原段落格式，替换文本 */
    private void replaceParagraphText(XWPFParagraph para, String newText) {
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) {
            para.createRun().setText(newText);
            return;
        }

        // 保存第一个非空 run 的格式作为模板
        XWPFRun templateRun = null;
        for (XWPFRun r : runs) {
            if (r.getText(0) != null && !r.getText(0).isEmpty()) {
                templateRun = r;
                break;
            }
        }
        if (templateRun == null) templateRun = runs.get(0);

        // 复制格式信息
        String fontFamily = templateRun.getFontFamily();
        Double fontSize = templateRun.getFontSizeAsDouble();
        boolean bold = templateRun.isBold();
        boolean italic = templateRun.isItalic();
        String color = templateRun.getColor();
        UnderlinePatterns underline = templateRun.getUnderline();

        // 清除所有旧 run
        for (int i = runs.size() - 1; i >= 0; i--) {
            para.removeRun(i);
        }

        // 按换行分 run，保持换行
        String[] lines = newText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            XWPFRun newRun = para.createRun();
            newRun.setText(lines[i]);
            if (fontFamily != null) newRun.setFontFamily(fontFamily);
            if (fontSize != null && fontSize > 0) newRun.setFontSize(fontSize);
            newRun.setBold(bold);
            newRun.setItalic(italic);
            if (color != null) newRun.setColor(color);
            if (underline != null) newRun.setUnderline(underline);

            if (i < lines.length - 1) {
                newRun.addBreak();
            }
        }
    }

    /** 模糊匹配：归一化后相似度 > 60% 则匹配 */
    private boolean fuzzyMatch(String text, String target) {
        String a = normalize(text);
        String b = normalize(target);
        if (a.contains(b) || b.contains(a)) return true;
        // 最长公共子串占比
        int lcs = longestCommonSubstring(a, b);
        return (double) lcs / Math.max(a.length(), b.length()) > 0.5;
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", "").replaceAll("[\\p{Punct}，。、；：！？\"\"''（）]", "").toLowerCase();
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
}
