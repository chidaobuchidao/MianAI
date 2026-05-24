-- V11__announcement.sql
-- 平台公告表，管理员发布，首页展示
CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '内容（支持Markdown/HTML/图片）',
    is_published TINYINT DEFAULT 1 COMMENT '0下架 1发布',
    created_by BIGINT COMMENT '发布人用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_published_time (is_published, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台公告';
