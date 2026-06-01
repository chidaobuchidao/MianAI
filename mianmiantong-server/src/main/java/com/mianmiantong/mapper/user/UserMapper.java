package com.mianmiantong.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** Atomic: increment quota_used by N steps without exceeding the user's daily quota. */
    @Update("""
        UPDATE sys_user
        SET quota_used = IF(quota_date = CURDATE(), COALESCE(quota_used, 0) + #{steps}, #{steps}),
            quota_date = CURDATE()
        WHERE id = #{userId}
          AND (
            quota_date IS NULL
            OR quota_date <> CURDATE()
            OR COALESCE(quota_used, 0) + #{steps} <= COALESCE(daily_quota, 10)
          )
        """)
    int incrementQuota(@Param("userId") Long userId, @Param("steps") int steps);
}
