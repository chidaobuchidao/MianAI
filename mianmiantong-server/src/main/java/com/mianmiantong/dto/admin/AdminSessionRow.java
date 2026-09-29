package com.mianmiantong.dto.admin;

import java.time.LocalDateTime;

/** 管理后台面试会话列表的一行。status 与 userName 是给界面显示的文本。 */
public record AdminSessionRow(
        Long id,
        Long userId,
        String position,
        Integer score,
        String status,
        LocalDateTime createTime,
        String userName) {
}
