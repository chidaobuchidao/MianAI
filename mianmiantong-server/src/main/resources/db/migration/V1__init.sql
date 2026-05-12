-- =============================================
-- 面面通 - AI模拟面试与智能刷题小程序
-- V1__init.sql - 初始化数据库表结构
-- =============================================

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    role TINYINT DEFAULT 0 COMMENT '0学生 1管理员',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 题目分类表
CREATE TABLE question_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目分类表';

-- 题库表
CREATE TABLE question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '1单选 2多选 3判断 4填空',
    title TEXT NOT NULL,
    options JSON COMMENT '选择题选项 [{label:A, content:...}]',
    answer VARCHAR(500) NOT NULL,
    analysis TEXT COMMENT '解析',
    difficulty TINYINT DEFAULT 1 COMMENT '1易 2中 3难',
    tags VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category_id),
    INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 试卷表
CREATE TABLE exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    duration INT COMMENT '考试时长(分钟)',
    total_score INT DEFAULT 100,
    status TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 试卷-题目关联表
CREATE TABLE exam_question (
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (exam_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 答题记录表
CREATE TABLE answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exam_id BIGINT,
    question_id BIGINT NOT NULL,
    user_answer VARCHAR(500),
    is_correct TINYINT COMMENT '0错误 1正确',
    score INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- 错题本
CREATE TABLE wrong_question (
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    wrong_count INT DEFAULT 1,
    last_wrong_time DATETIME,
    PRIMARY KEY (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- 用户收藏表
CREATE TABLE user_favorite (
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- AI面试会话表
CREATE TABLE interview_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    position VARCHAR(50) COMMENT '面试岗位',
    messages JSON COMMENT '对话记录 [{role:system/user/assistant, content:...}]',
    current_question_index INT DEFAULT 0,
    overall_score INT COMMENT 'AI综合评分',
    dimensions JSON COMMENT '各维度评分 [{name, score, comment}]',
    feedback TEXT COMMENT '总评',
    status TINYINT DEFAULT 0 COMMENT '0进行中 1已完成',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    finish_time DATETIME,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI面试会话表';

-- 插入默认分类
INSERT INTO question_category (name, icon, sort_order) VALUES
('计算机网络', 'network', 1),
('操作系统', 'os', 2),
('数据结构', 'data-structure', 3),
('算法', 'algorithm', 4),
('数据库', 'database', 5),
('Java', 'java', 6),
('设计模式', 'design-pattern', 7),
('计算机组成原理', 'computer-org', 8);
