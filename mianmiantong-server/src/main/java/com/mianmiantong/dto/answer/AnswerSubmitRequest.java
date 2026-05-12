package com.mianmiantong.dto.answer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitRequest {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    private String userAnswer;
    private Long examId;
}
