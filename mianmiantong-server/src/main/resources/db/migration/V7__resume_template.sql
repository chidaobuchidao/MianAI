-- V7__resume_template.sql - 简历模板表
CREATE TABLE IF NOT EXISTS resume_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description VARCHAR(500) COMMENT '模板描述',
    style_class VARCHAR(50) COMMENT '样式: professional/modern/minimal/tech',
    font_family VARCHAR(100) DEFAULT 'Microsoft YaHei' COMMENT '正文字体',
    heading_font VARCHAR(100) DEFAULT 'Microsoft YaHei' COMMENT '标题字体',
    heading_color VARCHAR(20) DEFAULT '1a3a6b' COMMENT '标题颜色',
    accent_color VARCHAR(20) DEFAULT '2b6ff2' COMMENT '强调色',
    bg_color VARCHAR(20) DEFAULT 'ffffff' COMMENT '背景色',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历模板表';

-- 种子数据
INSERT IGNORE INTO resume_template (name, description, style_class, font_family, heading_font, heading_color, accent_color, bg_color, sort_order) VALUES
('经典专业', '适合传统行业求职，稳重简洁', 'professional', 'Microsoft YaHei', 'SimHei', '1a3a6b', '2b6ff2', 'f8f9fa', 1),
('现代简约', '适合互联网行业，清爽大方', 'modern', 'Microsoft YaHei', 'SimHei', '0f172a', '6366f1', 'ffffff', 2),
('极简风格', '适合应届生，突出重点', 'minimal', 'Microsoft YaHei', 'SimHei', '333333', '000000', 'ffffff', 3),
('技术开发者', '突出技术栈和项目经验', 'tech', 'Consolas', 'SimHei', '1e293b', '059669', 'f5f7fa', 4);
