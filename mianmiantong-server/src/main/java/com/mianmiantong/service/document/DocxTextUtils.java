package com.mianmiantong.service.document;

/**
 * Shared text utilities for the document export pipeline.
 * Consolidates duplicated logic from {@link DocxPatchApplier} and
 * {@link TemplatePreservingExportService}.
 */
public class DocxTextUtils {

    private DocxTextUtils() {}

    /**
     * Strip Markdown formatting, returning plain text.
     * Handles: **bold** *italic* ## headings - lists `code` [links](url) ~~strikethrough~~
     */
    public static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        result = result.replaceAll("__(.+?)__", "$1");
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "$1");
        result = result.replaceAll("~~(.+?)~~", "$1");
        result = result.replaceAll("`(.+?)`", "$1");
        result = result.replaceAll("\\[(.+?)\\]\\([^)]*\\)", "$1");
        result = result.replaceAll("(?m)^#{1,6}\\s+", "");
        result = result.replaceAll("(?m)^[-*+]\\s+", "");
        result = result.replaceAll("(?m)^>\\s+", "");
        result = result.replaceAll("(?m)^\\d+\\.\\s+", "");
        return result.trim();
    }

    /** Normalize text for fuzzy matching: remove whitespace, punctuation, symbols; lowercase. */
    static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase().trim();
    }

    /** LCS-based similarity between two normalized strings (0.0 ~ 1.0). */
    static double similarity(String a, String b) {
        int lcs = lcsLength(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return maxLen > 0 ? (double) lcs / maxLen : 0;
    }

    /** Longest Common Subsequence length using space-optimized DP. */
    static int lcsLength(String a, String b) {
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                curr[j] = a.charAt(i - 1) == b.charAt(j - 1)
                    ? prev[j - 1] + 1
                    : Math.max(prev[j], curr[j - 1]);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }
}
