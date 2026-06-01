package com.mianmiantong.dto.paper;

import lombok.Data;

import java.util.List;

@Data
public class EvidenceRequest {
    private String queryText;
    private String focusText;
    private String model;
    private List<Chunk> chunks;

    @Data
    public static class Chunk {
        private Integer index;
        private Long chunkId;
        private Long paperId;
        private Integer chunkIndex;
        private String paperTitle;
        private String section;
        private String content;
        private Double score;
        private List<String> keywords;
    }
}
