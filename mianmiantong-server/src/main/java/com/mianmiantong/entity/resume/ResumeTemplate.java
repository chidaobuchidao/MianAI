package com.mianmiantong.entity.resume;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_template")
public class ResumeTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String styleClass;
    private String fontFamily;
    private String headingFont;
    private String headingColor;
    private String accentColor;
    private String bgColor;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createTime;
}
