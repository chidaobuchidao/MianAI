package com.mianmiantong.entity.resume;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String jobDescription;
    private String position;
    private String parsedText;
    private Integer parseStatus;
    private String docTaskId;
    private byte[] fileData;
    private LocalDateTime createTime;
}
