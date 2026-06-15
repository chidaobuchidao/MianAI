ALTER TABLE sys_user ADD COLUMN email VARCHAR(128) COMMENT '邮箱';
ALTER TABLE sys_user ADD COLUMN email_verified TINYINT DEFAULT 0 COMMENT '0未验证 1已验证';
ALTER TABLE sys_user ADD UNIQUE INDEX idx_email (email);
