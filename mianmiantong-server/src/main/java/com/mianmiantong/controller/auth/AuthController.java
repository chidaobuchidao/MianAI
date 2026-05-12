package com.mianmiantong.controller.auth;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.auth.LoginRequest;
import com.mianmiantong.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
