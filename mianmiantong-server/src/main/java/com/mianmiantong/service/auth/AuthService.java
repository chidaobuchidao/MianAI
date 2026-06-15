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
    private final EmailService emailService;
    private final VerificationCodeService codeService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ADMIN_USERNAME = "chidao";
    private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ADMIN_PASSWORD", "change-me");

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil,
                       EmailService emailService, VerificationCodeService codeService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.codeService = codeService;
    }

    // ==================== Email verification code ====================

    /** Send a 6-digit verification code to the given email. */
    public void sendVerificationCode(String email, String type) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (!email.matches("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        if (!"register".equals(type) && !"reset".equals(type)) {
            throw new IllegalArgumentException("无效的验证码类型");
        }

        // For registration, check if email is already taken
        if ("register".equals(type)) {
            User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
            );
            if (existing != null) {
                throw new IllegalArgumentException("该邮箱已被注册");
            }
        }
        // For password reset, check if email exists
        if ("reset".equals(type)) {
            User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
            );
            if (existing == null) {
                throw new IllegalArgumentException("该邮箱未注册");
            }
        }

        String code = codeService.generateAndStore(email, type);
        emailService.sendVerificationCode(email, type, code);
    }

    // ==================== Email registration ====================

    /** Register with email + verification code + password. */
    public LoginResponse registerByEmail(String email, String code, String password, String nickname) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }

        // Verify the code (does not delete it yet)
        codeService.verify(email, "register", code);

        // Check email uniqueness (defensive, already checked in sendCode but re-check)
        User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (existing != null) {
            throw new IllegalArgumentException("该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setEmailVerified(1);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : email.split("@")[0]);
        user.setUsername(email); // Use email as username for backward compatibility
        user.setOpenid("email_" + email.hashCode() + "_" + System.currentTimeMillis());
        user.setRole(0);
        user.setDailyQuota(10);
        user.setQuotaUsed(0);
        user.setQuotaDate(LocalDate.now());
        userMapper.insert(user);

        // Code consumed only after successful registration
        codeService.consumeCode(email, "register");

        String token = jwtUtil.generateToken(user.getId(), user.getOpenid(), 0);
        log.info("邮箱注册: email={}, userId={}", email, user.getId());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl(), user.getEmail());
    }

    // ==================== Email login ====================

    /** Login with email + password. */
    public LoginResponse loginByEmail(String email, String password) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (user == null) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole() != null ? user.getRole() : 0);
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl(), user.getEmail());
    }

    // ==================== Password reset ====================

    /** Reset password with email + verification code. */
    public void resetPassword(String email, String code, String newPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少6位");
        }

        codeService.verify(email, "reset", code);

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // Code consumed only after successful password reset
        codeService.consumeCode(email, "reset");

        log.info("密码重置成功: email={}, userId={}", email, user.getId());
    }

    // ==================== Original username registration ====================

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

    // ==================== Username login ====================

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
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl(), user.getEmail());
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
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl(), user.getEmail());
    }

    private String mockWechatLogin(String code) {
        if (code != null && code.length() > 20) {
            return "wx_" + code.substring(0, 28);
        }
        return "wx_test_openid_" + (code != null ? code.hashCode() & 0x7fffffff : System.currentTimeMillis() % 10000);
    }
}
