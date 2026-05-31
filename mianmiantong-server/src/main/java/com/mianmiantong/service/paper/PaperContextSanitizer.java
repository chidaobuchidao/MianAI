package com.mianmiantong.service.paper;

import com.mianmiantong.dto.paper.ContextChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库上下文安全清洗器。
 * <p>
 * 隐私要求：后端日志不得打印 contextChunks.content 正文。
 * 此类统一执行数量/长度限制和 null 清洗，避免各 Service 重复实现。
 */
@Slf4j
public final class PaperContextSanitizer {

    private static final int MAX_CHUNKS = 5;
    private static final int MAX_SINGLE_CONTENT_LENGTH = 1000;
    private static final int MAX_TOTAL_LENGTH = 4000;

    private PaperContextSanitizer() {}

    /**
     * 清洗并限制 contextChunks。
     * 返回清洗后的列表；如果输入为空或全部无效则返回空列表。
     * 日志仅记录 chunk 数量和总字符数，不打印正文。
     */
    public static List<ContextChunk> sanitize(List<ContextChunk> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        List<ContextChunk> cleaned = new ArrayList<>();
        int totalLength = 0;

        for (ContextChunk chunk : input) {
            if (cleaned.size() >= MAX_CHUNKS) break;
            if (chunk == null) continue;

            String title = normalizeTitle(chunk.getPaperTitle());
            String section = normalizeOptionalText(chunk.getSection());
            String content = chunk.getContent();

            if (content == null || content.isBlank()) continue;
            content = content.trim();

            // 单 chunk 长度限制
            if (content.length() > MAX_SINGLE_CONTENT_LENGTH) {
                content = content.substring(0, MAX_SINGLE_CONTENT_LENGTH);
            }

            // 总长度限制
            if (totalLength + content.length() > MAX_TOTAL_LENGTH) {
                int remaining = MAX_TOTAL_LENGTH - totalLength;
                if (remaining <= 0) break;
                content = content.substring(0, remaining);
            }

            ContextChunk c = new ContextChunk();
            c.setPaperTitle(title);
            c.setSection(section);
            c.setContent(content);
            cleaned.add(c);
            totalLength += content.length();
        }

        // 日志脱敏：只记录数量和总字符数
        log.info("KB context sanitized: {} chunks, {} chars total", cleaned.size(), totalLength);
        return cleaned;
    }

    /**
     * 将清洗后的 contextChunks 格式化为 prompt 文本块。
     * 如果列表为空返回空字符串。
     */
    public static String formatForPrompt(List<ContextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("以下是从知识库中检索到的相关内容，请参考：\n\n");
        int idx = 1;
        for (ContextChunk c : chunks) {
            sb.append("[参考").append(idx).append("] ").append(c.getPaperTitle());
            if (c.getSection() != null && !c.getSection().isBlank()) {
                sb.append("【").append(c.getSection()).append("】");
            }
            sb.append("\n").append(c.getContent()).append("\n\n---\n\n");
            idx++;
        }
        return sb.toString();
    }

    private static String normalizeTitle(String title) {
        String normalized = normalizeOptionalText(title);
        return normalized != null ? normalized : "未知论文";
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
