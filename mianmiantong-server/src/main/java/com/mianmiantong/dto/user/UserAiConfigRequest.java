package com.mianmiantong.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserAiConfigRequest {
    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    private String provider = "deepseek";
}
