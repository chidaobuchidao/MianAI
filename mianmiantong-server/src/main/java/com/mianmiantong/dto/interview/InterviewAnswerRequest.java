package com.mianmiantong.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewAnswerRequest {
    @NotBlank(message = "回答不能为空")
    private String answer;
}
