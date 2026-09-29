package com.mianmiantong.dto.admin;

import java.time.LocalDateTime;

/** 管理后台用户列表的一行。role 是给界面显示的中文标签；配额字段未设置时为 null。 */
public record AdminUserRow(
        Long id,
        String nickname,
        String role,
        boolean hasApiKey,
        boolean knowledgeBaseEnabled,
        Integer dailyQuota,
        Integer quotaUsed,
        LocalDateTime createTime,
        long interviewCount) {
}
