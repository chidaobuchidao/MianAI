package com.mianmiantong.dto.auth;

import lombok.Data;

@Data
public class RegisterByEmailRequest {
    private String email;
    private String code;
    private String password;
    private String nickname;
}
