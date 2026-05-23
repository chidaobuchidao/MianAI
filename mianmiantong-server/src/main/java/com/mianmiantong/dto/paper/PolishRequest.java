package com.mianmiantong.dto.paper;

import lombok.Data;

@Data
public class PolishRequest {
    private String text;
    private String taskType = "章节正文";
    private String polishType = "full";
    private String executionMode = "标准模式";
    private String topic = "";
    private String notes = "";
}
