package com.mianmiantong.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewStartRequest {
    @NotBlank(message = "面试岗位不能为空")
    private String position;
    private Long resumeId;
}
