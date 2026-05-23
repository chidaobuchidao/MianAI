package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class PlagiarismReduceRequest {
    private String text;
    private String sourceText = "";
    private String mode = "medium";

    @Data
    public static class ReportAnnotation {
        private int paragraphId;
        private String sourceExcerpt;
        private boolean includeInRun;
        private String riskLevel;
        private int start;
        private int end;
    }
}
