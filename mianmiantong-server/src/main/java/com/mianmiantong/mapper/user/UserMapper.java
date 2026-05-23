package com.mianmiantong.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** Atomic: increment quota_used by N steps. Same-day +=N, new-day =N */
    @Update("UPDATE sys_user SET quota_used = IF(quota_date = CURDATE(), quota_used + #{steps}, #{steps}), quota_date = CURDATE() WHERE id = #{userId}")
    void incrementQuota(@Param("userId") Long userId, @Param("steps") int steps);
}
