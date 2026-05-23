package com.mianmiantong.dto.paper;

import lombok.Data;

@Data
public class AiReduceRequest {
    private String text;
    private String mode = "light";
}
