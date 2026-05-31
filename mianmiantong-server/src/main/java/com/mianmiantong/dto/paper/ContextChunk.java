package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class ContextChunk {
    private String paperTitle;
    private String section;
    private String content;
    private Double score;
    private List<String> keywords;
    private Long chunkId;
    private Long paperId;
    private Integer chunkIndex;
}
