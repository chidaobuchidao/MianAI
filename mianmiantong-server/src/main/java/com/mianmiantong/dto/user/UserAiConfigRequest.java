package com.mianmiantong.dto.user;

import lombok.Data;

@Data
public class UserAiConfigRequest {
    private String apiKey;
    private String provider = "deepseek";
    private String model;
    private String preferredModel;
    private String customEndpoint;
}
