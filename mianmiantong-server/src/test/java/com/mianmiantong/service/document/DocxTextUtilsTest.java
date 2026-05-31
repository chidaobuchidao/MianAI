package com.mianmiantong.service.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocxTextUtilsTest {

    @Nested
    @DisplayName("stripMarkdown")
    class StripMarkdown {

        @Test
        @DisplayName("removes bold markers")
        void bold() {
            assertThat(DocxTextUtils.stripMarkdown("**bold text**")).isEqualTo("bold text");
        }

        @Test
        @DisplayName("removes italic markers")
        void italic() {
            assertThat(DocxTextUtils.stripMarkdown("*italic text*")).isEqualTo("italic text");
        }

        @Test
        @DisplayName("removes inline code")
        void inlineCode() {
            assertThat(DocxTextUtils.stripMarkdown("`code`")).isEqualTo("code");
        }

        @Test
        @DisplayName("removes links, keeps text")
        void links() {
            assertThat(DocxTextUtils.stripMarkdown("[click here](https://example.com)")).isEqualTo("click here");
        }

        @Test
        @DisplayName("removes heading markers")
        void headings() {
            assertThat(DocxTextUtils.stripMarkdown("## Title")).isEqualTo("Title");
        }

        @Test
        @DisplayName("removes strikethrough")
        void strikethrough() {
            assertThat(DocxTextUtils.stripMarkdown("~~deleted~~")).isEqualTo("deleted");
        }

        @Test
        @DisplayName("returns null for null input")
        void nullInput() {
            assertThat(DocxTextUtils.stripMarkdown(null)).isNull();
        }

        @Test
        @DisplayName("returns empty for empty input")
        void emptyInput() {
            assertThat(DocxTextUtils.stripMarkdown("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("normalize")
    class Normalize {

        @Test
        @DisplayName("removes punctuation, whitespace, symbols; lowercases")
        void basic() {
            assertThat(DocxTextUtils.normalize("Hello, World!")).isEqualTo("helloworld");
        }

        @Test
        @DisplayName("handles Chinese text")
        void chinese() {
            assertThat(DocxTextUtils.normalize("你好，世界！")).isEqualTo("你好世界");
        }

        @Test
        @DisplayName("returns empty for null")
        void nullInput() {
            assertThat(DocxTextUtils.normalize(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("similarity")
    class Similarity {

        @Test
        @DisplayName("identical strings return 1.0")
        void identical() {
            assertThat(DocxTextUtils.similarity("hello", "hello")).isEqualTo(1.0);
        }

        @Test
        @DisplayName("completely different strings return low score")
        void different() {
            assertThat(DocxTextUtils.similarity("abc", "xyz")).isLessThan(0.5);
        }

        @Test
        @DisplayName("empty strings return 0")
        void empty() {
            assertThat(DocxTextUtils.similarity("", "")).isEqualTo(0);
        }

        @Test
        @DisplayName("one empty string returns 0")
        void oneEmpty() {
            assertThat(DocxTextUtils.similarity("abc", "")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("lcsLength")
    class LcsLength {

        @Test
        @DisplayName("known sequences")
        void known() {
            assertThat(DocxTextUtils.lcsLength("ABCBDAB", "BDCAB")).isEqualTo(4);
        }

        @Test
        @DisplayName("identical strings")
        void identical() {
            assertThat(DocxTextUtils.lcsLength("hello", "hello")).isEqualTo(5);
        }

        @Test
        @DisplayName("no common subsequence")
        void noCommon() {
            assertThat(DocxTextUtils.lcsLength("abc", "xyz")).isEqualTo(0);
        }
    }
}
