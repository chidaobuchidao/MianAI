package com.mianmiantong.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String email;
    private boolean needBindEmail;

    public LoginResponse(String token, Long userId, String nickname, String avatarUrl) {
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.email = null;
        this.needBindEmail = false;
    }

    public LoginResponse(String token, Long userId, String nickname, String avatarUrl, String email) {
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.email = email;
        this.needBindEmail = (email == null || email.isBlank());
    }
}
