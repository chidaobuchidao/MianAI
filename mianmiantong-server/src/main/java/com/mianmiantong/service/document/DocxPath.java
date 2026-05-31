package com.mianmiantong.service.document;

/**
 * Stable paragraph address within a DOCX document.
 * Replaces fragile index-based offset with human-readable path strings.
 *
 * <pre>
 * body.p[0]                        — body paragraph 0
 * body.table[0].row[1].cell[2].p[0] — table 0, row 1, cell 2, paragraph 0
 * header[0].p[0]                   — header 0, paragraph 0
 * footer[0].p[0]                   — footer 0, paragraph 0
 * </pre>
 */
public record DocxPath(String location, String pathString) {

    /** body paragraph at position i */
    public static DocxPath body(int paraIndex) {
        return new DocxPath("body", "body.p[" + paraIndex + "]");
    }

    /** paragraph inside a table cell */
    public static DocxPath tableCell(int tableIdx, int rowIdx, int cellIdx, int paraIdx) {
        String path = "body.table[" + tableIdx + "].row[" + rowIdx + "].cell[" + cellIdx + "].p[" + paraIdx + "]";
        return new DocxPath("table[" + tableIdx + "].row[" + rowIdx + "].cell[" + cellIdx + "]", path);
    }

    /** header paragraph */
    public static DocxPath header(int headerIdx, int paraIdx) {
        return new DocxPath("header[" + headerIdx + "]", "header[" + headerIdx + "].p[" + paraIdx + "]");
    }

    /** footer paragraph */
    public static DocxPath footer(int footerIdx, int paraIdx) {
        return new DocxPath("footer[" + footerIdx + "]", "footer[" + footerIdx + "].p[" + paraIdx + "]");
    }

    /** text box paragraph (floating shape) */
    public static DocxPath textBox(int paraIdx) {
        return new DocxPath("txbx", "txbx[" + paraIdx + "].p[0]");
    }
}
