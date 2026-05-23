package com.mianmiantong.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String username;
    private String passwordHash;
    private Integer role;
    private Integer dailyQuota;
    private Integer quotaUsed;
    private LocalDate quotaDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
