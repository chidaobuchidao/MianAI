package com.mianmiantong.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.common.JwtUtil;
import com.mianmiantong.dto.auth.LoginRequest;
import com.mianmiantong.dto.auth.LoginResponse;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.mapper.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ADMIN_USERNAME = "chidao";
    private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ADMIN_PASSWORD", "change-me");

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /** Register new user with username + password */
    public LoginResponse register(String username, String password, String nickname) {
        if (username == null || username.isBlank() || username.length() < 2) {
            throw new IllegalArgumentException("用户名至少2个字符");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }
        User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        // Check nickname uniqueness
        if (nickname != null && !nickname.isBlank()) {
            User sameName = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getNickname, nickname)
            );
            if (sameName != null) {
                throw new IllegalArgumentException("昵称已被使用，请换一个");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setOpenid("pwd_" + username + "_" + System.currentTimeMillis());
        user.setRole(0);
        user.setDailyQuota(10);
        user.setQuotaUsed(0);
        user.setQuotaDate(LocalDate.now());
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole() != null ? user.getRole() : 0);
        log.info("新用户注册: username={}, userId={}", username, user.getId());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }

    /** Username + password login */
    public LoginResponse login(String username, String password) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole() != null ? user.getRole() : 0);
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }
    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == 1;
    }

    // Keep wechat mock login for backward compatibility
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
            user.setRole(0);
            userMapper.insert(user);
            log.info("新用户注册(微信): openid={}, userId={}", openid, user.getId());
        }

        String token = jwtUtil.generateToken(user.getId(), openid, user.getRole() != null ? user.getRole() : 0);
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }

    private String mockWechatLogin(String code) {
        if (code != null && code.length() > 20) {
            return "wx_" + code.substring(0, 28);
        }
        return "wx_test_openid_" + (code != null ? code.hashCode() & 0x7fffffff : System.currentTimeMillis() % 10000);
    }
}
