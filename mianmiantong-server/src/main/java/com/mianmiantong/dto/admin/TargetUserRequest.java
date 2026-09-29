package com.mianmiantong.dto.admin;

import jakarta.validation.constraints.NotNull;

/** 只需指定目标用户的管理操作：切换管理员、切换知识库、删除用户。 */
public record TargetUserRequest(
        @NotNull(message = "userId不能为空") Long userId) {
}
