package com.mianmiantong.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 设置用户每日免费次数上限。 */
public record SetLimitRequest(
        @NotNull(message = "userId不能为空") Long userId,
        @NotNull(message = "limit不能为空")
        @Min(value = 0, message = "limit不能为负数") Integer limit) {
}
