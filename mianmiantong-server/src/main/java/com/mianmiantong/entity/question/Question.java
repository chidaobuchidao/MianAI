package com.mianmiantong.entity.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Integer type;
    private String title;
    @TableField("`options`")
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private String tags;
    private LocalDateTime createTime;

    /** 非表字段 - 分类名称 */
    @TableField(exist = false)
    private String categoryName;
}
