-- V4__resume.sql - 简历表
CREATE TABLE resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型 pdf/docx/jpg/png',
    file_size BIGINT COMMENT '文件大小(字节)',
    job_description TEXT NOT NULL COMMENT '目标岗位JD',
    position VARCHAR(50) COMMENT '目标岗位名称',
    parsed_text MEDIUMTEXT COMMENT '解析后的结构化文本',
    parse_status TINYINT DEFAULT 0 COMMENT '0解析中 1完成 -1失败',
    doc_task_id VARCHAR(100) COMMENT '阿里云任务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';
