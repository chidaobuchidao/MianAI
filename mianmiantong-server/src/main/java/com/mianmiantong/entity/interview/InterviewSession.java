package com.mianmiantong.entity.interview;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String position;
    private String messages;
    private Integer currentQuestionIndex;
    private Integer overallScore;
    private String dimensions;
    private String feedback;
    private Integer status;
    private String model;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
