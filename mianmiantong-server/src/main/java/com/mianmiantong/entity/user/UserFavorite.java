package com.mianmiantong.entity.user;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_favorite")
public class UserFavorite {
    private Long userId;
    private Long questionId;
    private LocalDateTime createTime;
}
