package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class PaperExportRequest {
    private List<ParagraphMapping> paragraphs;

    @Data
    public static class ParagraphMapping {
        private int index;
        private String text;
    }
}
