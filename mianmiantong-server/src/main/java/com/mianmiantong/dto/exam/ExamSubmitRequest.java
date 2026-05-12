package com.mianmiantong.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ExamSubmitRequest {
    @NotNull(message = "答案列表不能为空")
    private List<ExamAnswer> answers;

    @Data
    public static class ExamAnswer {
        private Long questionId;
        private String userAnswer;
    }
}
