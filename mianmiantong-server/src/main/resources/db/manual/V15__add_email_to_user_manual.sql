-- ============================================================
-- V15: 邮箱注册/接码功能 — 手动执行脚本
-- 适用：生产环境禁用了 Flyway 时，直接用此 SQL
-- 执行前建议先备份 sys_user 表
-- ============================================================

-- 1. 添加邮箱字段
ALTER TABLE sys_user
    ADD COLUMN email VARCHAR(128) COMMENT '邮箱' AFTER knowledge_base_enabled,
    ADD COLUMN email_verified TINYINT DEFAULT 0 COMMENT '0未验证 1已验证' AFTER email;

-- 2. 邮箱唯一索引（已有邮箱数据的先确认无重复再执行）
ALTER TABLE sys_user ADD UNIQUE INDEX idx_email (email);
