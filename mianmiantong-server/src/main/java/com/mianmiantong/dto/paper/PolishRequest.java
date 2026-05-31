package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class PolishRequest {
    private String text;
    private String taskType = "章节正文";
    private String polishType = "full";
    private String topic = "";
    private String notes = "";
    private String language = "zh";
    private String model;
    private List<ContextChunk> contextChunks;
}
