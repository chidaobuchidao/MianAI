package com.mianmiantong.dto.paper;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EvidenceResponse {
    private List<Evidence> evidences = new ArrayList<>();

    @Data
    public static class Evidence {
        private Integer index;
        private Long chunkId;
        private Long paperId;
        private Integer chunkIndex;
        private String paperTitle;
        private String section;
        private String content;
        private Double score;
        private List<String> keywords;
        private String supportLevel;
        private String confidence;
        private String supportedClaim;
        private String reason;
    }
}
