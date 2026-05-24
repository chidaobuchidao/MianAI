package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class AiReduceRequest {
    private String text;
    private String mode = "light";
    private String model;
    private List<FlaggedSentence> flaggedSentences;

    @Data
    public static class FlaggedSentence {
        private String text;
        private String reason;
        private String suggestion;
    }
}
