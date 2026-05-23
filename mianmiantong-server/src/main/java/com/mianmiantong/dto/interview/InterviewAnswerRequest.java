package com.mianmiantong.dto.interview;

import lombok.Data;

@Data
public class InterviewAnswerRequest {
    private String answer;
    private String code;
    private String codeLang;
    private String codeFile;
}
