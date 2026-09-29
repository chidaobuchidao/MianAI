package com.mianmiantong.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 设置用户今日剩余免费次数；服务端据此反推已用次数。 */
public record SetQuotaRequest(
        @NotNull(message = "userId不能为空") Long userId,
        @NotNull(message = "remaining不能为空")
        @Min(value = 0, message = "remaining不能为负数") Integer remaining) {
}
