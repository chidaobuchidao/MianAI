package com.mianmiantong.service.paper;

import com.mianmiantong.dto.paper.ContextChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaperContextSanitizerTest {

    @Test
    @DisplayName("returns empty list for null or empty input")
    void emptyInput() {
        assertThat(PaperContextSanitizer.sanitize(null)).isEmpty();
        assertThat(PaperContextSanitizer.sanitize(List.of())).isEmpty();
    }

    @Test
    @DisplayName("skips null chunks and blank content")
    void skipsInvalidChunks() {
        List<ContextChunk> chunks = new ArrayList<>();
        chunks.add(null);
        chunks.add(chunk("  ", "  ", "   "));
        chunks.add(chunk("  Paper A  ", "  Methods  ", "  valid content  "));

        List<ContextChunk> sanitized = PaperContextSanitizer.sanitize(chunks);

        assertThat(sanitized).hasSize(1);
        assertThat(sanitized.get(0).getPaperTitle()).isEqualTo("Paper A");
        assertThat(sanitized.get(0).getSection()).isEqualTo("Methods");
        assertThat(sanitized.get(0).getContent()).isEqualTo("valid content");
    }

    @Test
    @DisplayName("limits chunk count")
    void limitsChunkCount() {
        List<ContextChunk> input = List.of(
            chunk("P1", null, "one"),
            chunk("P2", null, "two"),
            chunk("P3", null, "three"),
            chunk("P4", null, "four"),
            chunk("P5", null, "five"),
            chunk("P6", null, "six")
        );

        assertThat(PaperContextSanitizer.sanitize(input))
            .extracting(ContextChunk::getPaperTitle)
            .containsExactly("P1", "P2", "P3", "P4", "P5");
    }

    @Test
    @DisplayName("limits single chunk content length")
    void limitsSingleChunkLength() {
        List<ContextChunk> sanitized = PaperContextSanitizer.sanitize(List.of(
            chunk("Paper", null, "a".repeat(1200))
        ));

        assertThat(sanitized).hasSize(1);
        assertThat(sanitized.get(0).getContent()).hasSize(1000);
    }

    @Test
    @DisplayName("limits total content length")
    void limitsTotalContentLength() {
        List<ContextChunk> input = List.of(
            chunk("P1", null, "a".repeat(1000)),
            chunk("P2", null, "b".repeat(1000)),
            chunk("P3", null, "c".repeat(1000)),
            chunk("P4", null, "d".repeat(1000)),
            chunk("P5", null, "e".repeat(1000))
        );

        List<ContextChunk> sanitized = PaperContextSanitizer.sanitize(input);

        assertThat(sanitized).hasSize(4);
        assertThat(sanitized.stream().mapToInt(c -> c.getContent().length()).sum()).isEqualTo(4000);
    }

    @Test
    @DisplayName("formats sanitized chunks for prompts")
    void formatsForPrompt() {
        String prompt = PaperContextSanitizer.formatForPrompt(List.of(
            chunk("Paper", "Conclusion", "context")
        ));

        assertThat(prompt).contains("[参考1] Paper【Conclusion】");
        assertThat(prompt).contains("context");
    }

    private static ContextChunk chunk(String title, String section, String content) {
        ContextChunk chunk = new ContextChunk();
        chunk.setPaperTitle(title);
        chunk.setSection(section);
        chunk.setContent(content);
        return chunk;
    }
}
