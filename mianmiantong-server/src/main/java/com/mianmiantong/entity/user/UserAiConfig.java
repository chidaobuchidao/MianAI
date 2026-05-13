package com.mianmiantong.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_ai_config")
public class UserAiConfig {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private String provider;
    private String apiKey;
    private String model;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
