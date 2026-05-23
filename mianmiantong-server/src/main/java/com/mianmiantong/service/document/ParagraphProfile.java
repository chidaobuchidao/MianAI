package com.mianmiantong.service.document;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * 段落的格式快照，用于格式保留导出时的段落→格式映射。
 * 只记录首个非空 run 的格式，覆盖学术论文 95% 的段落场景。
 */
public record ParagraphProfile(
    int index,
    String text,
    String styleId,
    String fontFamily,
    String eastAsiaFont,
    Double fontSize,
    boolean bold,
    boolean italic,
    String color,
    String alignment,
    Double indentLeft,
    Double firstLineIndent
) {

    /** 从 XWPFParagraph 提取格式快照 */
    public static ParagraphProfile from(int index, XWPFParagraph para) {
        String text = para.getText();
        if (text == null) text = "";

        String styleId = para.getStyleID();
        String fontFamily = null;
        String eastAsiaFont = null;
        Double fontSize = null;
        boolean bold = false;
        boolean italic = false;
        String color = null;

        for (XWPFRun run : para.getRuns()) {
            String runText = run.getText(0);
            if (runText != null && !runText.isEmpty()) {
                fontFamily = run.getFontFamily();
                eastAsiaFont = run.getFontFamily(XWPFRun.FontCharRange.eastAsia);
                if (run.getFontSizeAsDouble() != null && run.getFontSizeAsDouble() > 0) {
                    fontSize = run.getFontSizeAsDouble();
                }
                bold = run.isBold();
                italic = run.isItalic();
                color = run.getColor();
                break;
            }
        }
        if (fontFamily == null && !para.getRuns().isEmpty()) {
            XWPFRun first = para.getRuns().get(0);
            fontFamily = first.getFontFamily();
            eastAsiaFont = first.getFontFamily(XWPFRun.FontCharRange.eastAsia);
            if (first.getFontSizeAsDouble() != null && first.getFontSizeAsDouble() > 0) {
                fontSize = first.getFontSizeAsDouble();
            }
            bold = first.isBold();
            italic = first.isItalic();
            color = first.getColor();
        }

        String alignment = "LEFT";
        ParagraphAlignment pa = para.getAlignment();
        if (pa != null) alignment = pa.toString();

        double indentLeft = para.getIndentationLeft() / 20.0;
        double firstLine = para.getIndentationFirstLine() / 20.0;

        return new ParagraphProfile(
            index, text, styleId, fontFamily, eastAsiaFont,
            fontSize, bold, italic, color, alignment, indentLeft, firstLine
        );
    }

    public boolean isRewritable() {
        if (text == null || text.isBlank()) return false;
        return text.trim().length() > 5;
    }
}
