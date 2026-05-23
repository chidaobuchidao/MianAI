package com.mianmiantong.service.document;

import com.mianmiantong.dto.paper.PaperExportRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于原始 .docx 模板的格式保留导出服务。
 * 采用段落 ID 标记法：逐段匹配改写结果，保留原文档格式。
 */
@Slf4j
@Service
public class TemplatePreservingExportService {

    /** 从原始 DOCX 解析所有可改写段落的格式快照 */
    public List<ParagraphProfile> parseParagraphs(byte[] originalDocx) {
        List<ParagraphProfile> profiles = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(originalDocx))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (int i = 0; i < paragraphs.size(); i++) {
                XWPFParagraph para = paragraphs.get(i);
                String text = para.getText();
                if (text == null || text.isBlank()) continue;
                ParagraphProfile profile = ParagraphProfile.from(i, para);
                if (profile.isRewritable()) {
                    profiles.add(profile);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse paragraphs from DOCX", e);
            throw new RuntimeException("Failed to parse paragraphs from DOCX", e);
        }
        return profiles;
    }

    /** 构造逐段改写 prompt */
    public String buildRewritePrompt(List<ParagraphProfile> paragraphs, String taskDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append(taskDescription).append("\n\n");
        sb.append("你必须逐段处理以下文本。每个段落以 [P{n}] 标记开头。\n");
        sb.append("要求：1) 逐段处理，保持 [P{n}] 标记不变；2) 段落数量必须与输入一致；3) 不要合并或拆分段落。\n\n");
        for (ParagraphProfile p : paragraphs) {
            sb.append("[P").append(p.index()).append("] ").append(p.text()).append("\n\n");
        }
        return sb.toString();
    }

    /** 解析 AI 返回的逐段改写结果 */
    public Map<Integer, String> parseRewriteResponse(String aiResponse) {
        Map<Integer, String> result = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("\\[P(\\d+)\\](.*?)(?=\\[P\\d+\\]|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        while (matcher.find()) {
            int idx = Integer.parseInt(matcher.group(1));
            String text = matcher.group(2).trim();
            result.put(idx, text);
        }
        return result;
    }

    /** 将改写后的段落写回原 DOCX，保留所有格式 */
    public byte[] writeBack(byte[] originalDocx, Map<Integer, String> rewrittenParagraphs) {
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

            // 尝试替换表格内文本
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
        return writeBack(originalDocx, paraMap);
    }

    /** 保留原段落格式，替换文本 */
    void replaceParagraphText(XWPFParagraph para, String newText) {
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) {
            para.createRun().setText(newText);
            return;
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
            if (i < lines.length - 1) newRun.addBreak();
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
}
