-- V10__interview_coding_report.sql
-- 笔试报告独立字段，与面试报告互不覆盖
ALTER TABLE interview_session
    ADD COLUMN coding_score INT DEFAULT 0 COMMENT '笔试评分(1-10)',
    ADD COLUMN coding_dimensions TEXT COMMENT '笔试维度评分JSON',
    ADD COLUMN coding_feedback TEXT COMMENT '笔试总评',
    ADD COLUMN coding_suggestion TEXT COMMENT '笔试提升建议';
