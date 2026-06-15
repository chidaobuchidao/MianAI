package com.mianmiantong.controller.auth;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.auth.LoginRequest;
import com.mianmiantong.dto.auth.RegisterByEmailRequest;
import com.mianmiantong.dto.auth.ResetPasswordRequest;
import com.mianmiantong.dto.auth.SendCodeRequest;
import com.mianmiantong.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** WeChat mock login (kept for mini-program compatibility) */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** Username + password login */
    @PostMapping("/login/password")
    public Result<?> passwordLogin(@RequestBody Map<String, String> body) {
        return Result.ok(authService.login(
            body.getOrDefault("username", ""),
            body.getOrDefault("password", "")
        ));
    }

    /** Email + password login */
    @PostMapping("/login/email")
    public Result<?> emailLogin(@RequestBody Map<String, String> body) {
        return Result.ok(authService.loginByEmail(
            body.getOrDefault("email", ""),
            body.getOrDefault("password", "")
        ));
    }

    /** Register new account (username + password, kept for backward compatibility) */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        return Result.ok(authService.register(
            body.getOrDefault("username", ""),
            body.getOrDefault("password", ""),
            body.getOrDefault("nickname", "")
        ));
    }

    /** Register with email + verification code */
    @PostMapping("/register/email")
    public Result<?> emailRegister(@RequestBody RegisterByEmailRequest request) {
        return Result.ok(authService.registerByEmail(
            request.getEmail(),
            request.getCode(),
            request.getPassword(),
            request.getNickname()
        ));
    }

    /** Send verification code to email */
    @PostMapping("/send-code")
    public Result<?> sendCode(@RequestBody SendCodeRequest request) {
        authService.sendVerificationCode(request.getEmail(), request.getType());
        return Result.ok();
    }

    /** Reset password with email + verification code */
    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return Result.ok();
    }
}
