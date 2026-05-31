package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class PlagiarismReduceRequest {
    private String text;
    private String sourceText = "";
    private String mode = "medium";
    private String model;
    private List<ReportAnnotation> annotations;
    private List<ContextChunk> contextChunks;

    @Data
    public static class ReportAnnotation {
        private String text;           // 标注的文本内容
        private String riskLevel;      // high / medium / low
        private int paragraphId;
        private String sourceExcerpt;
        private boolean includeInRun;
        private int start;
        private int end;
    }
}
