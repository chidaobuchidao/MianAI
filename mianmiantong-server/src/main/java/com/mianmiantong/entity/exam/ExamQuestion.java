package com.mianmiantong.entity.exam;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_question")
public class ExamQuestion {
    private Long examId;
    private Long questionId;
    private Integer sortOrder;
}
