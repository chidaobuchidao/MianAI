-- Run this manually when Flyway is disabled.
-- Adds the admin-grant flag for paper knowledge base access.
ALTER TABLE sys_user
ADD COLUMN knowledge_base_enabled TINYINT DEFAULT 0 COMMENT '是否允许无自有API Key使用论文知识库';
