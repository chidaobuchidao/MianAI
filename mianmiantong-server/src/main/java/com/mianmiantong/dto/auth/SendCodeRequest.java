package com.mianmiantong.dto.auth;

import lombok.Data;

@Data
public class SendCodeRequest {
    private String email;
    /** register | reset */
    private String type;
}
