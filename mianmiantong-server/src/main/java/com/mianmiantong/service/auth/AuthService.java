package com.mianmiantong.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.common.JwtUtil;
import com.mianmiantong.dto.auth.LoginRequest;
import com.mianmiantong.dto.auth.LoginResponse;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.mapper.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        String openid = mockWechatLogin(request.getCode());

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + System.currentTimeMillis() % 10000);
            user.setAvatarUrl(request.getAvatarUrl());
            userMapper.insert(user);
            log.info("新用户注册: openid={}, userId={}", openid, user.getId());
        }

        String token = jwtUtil.generateToken(user.getId(), openid);

        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }

    private String mockWechatLogin(String code) {
        if (code != null && code.length() > 20) {
            return "wx_" + code.substring(0, 28);
        }
        return "wx_test_openid_" + (code != null ? code.hashCode() & 0x7fffffff : System.currentTimeMillis() % 10000);
    }
}
