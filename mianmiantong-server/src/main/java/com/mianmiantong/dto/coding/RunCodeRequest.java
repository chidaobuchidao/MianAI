package com.mianmiantong.dto.coding;

import lombok.Data;

@Data
public class RunCodeRequest {
    private String code;
    private String language;
    private String stdin;
}
