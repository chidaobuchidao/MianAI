-- V9__user_ai_config_model.sql
ALTER TABLE user_ai_config ADD COLUMN model VARCHAR(30) DEFAULT 'deepseek-v4-flash' COMMENT '默认模型';
