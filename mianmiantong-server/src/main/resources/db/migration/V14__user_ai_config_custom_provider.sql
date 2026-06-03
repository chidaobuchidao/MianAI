-- V14__user_ai_config_custom_provider.sql
-- 添加用户自定义提供者支持
ALTER TABLE user_ai_config ADD COLUMN custom_endpoint VARCHAR(512) DEFAULT NULL COMMENT '自定义 API 端点';
ALTER TABLE user_ai_config ADD COLUMN preferred_model VARCHAR(64) DEFAULT NULL COMMENT '用户偏好模型';
