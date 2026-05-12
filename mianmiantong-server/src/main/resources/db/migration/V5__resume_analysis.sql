-- V5__resume_analysis.sql - 简历分析报告表
CREATE TABLE resume_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL UNIQUE COMMENT '简历ID',
    overall_score INT COMMENT '综合评分1-10',
    dimensions JSON COMMENT '五维度评分 [{name,score,comment}]',
    missing_keywords JSON COMMENT '缺失关键词 ["Redis","Docker"]',
    optimized_text MEDIUMTEXT COMMENT '优化后完整简历(Markdown)',
    highlights JSON COMMENT '优化对比 [{section,before,after,reason}]',
    interview_questions JSON COMMENT '面试追问 ["问题1","问题2"]',
    suggestion TEXT COMMENT '总体提升建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历分析报告表';
