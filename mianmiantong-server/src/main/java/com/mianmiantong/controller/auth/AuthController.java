package com.mianmiantong.controller.auth;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.auth.LoginRequest;
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

    /** Register new account */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        return Result.ok(authService.register(
            body.getOrDefault("username", ""),
            body.getOrDefault("password", ""),
            body.getOrDefault("nickname", "")
        ));
    }
}
