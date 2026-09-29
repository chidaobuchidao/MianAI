package com.mianmiantong.dto.admin;

/** 管理后台首页的系统概况。 */
public record AdminStatus(long totalUsers, long totalSessions, boolean hasSystemKey, long usersWithKey) {
}
