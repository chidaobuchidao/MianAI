package com.mianmiantong.entity.resume;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_analysis")
public class ResumeAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Integer overallScore;
    private String dimensions;
    private String missingKeywords;
    private String optimizedText;
    private String highlights;
    private String interviewQuestions;
    private String suggestion;
    private LocalDateTime createTime;
}
