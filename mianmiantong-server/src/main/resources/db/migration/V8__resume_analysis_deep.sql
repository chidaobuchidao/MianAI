-- V8__resume_analysis_deep.sql - 深度优化重试与断点续传
ALTER TABLE resume_analysis
  ADD COLUMN deep_status INT DEFAULT 0 COMMENT '深度优化状态: 0待优化 1进行中 2已完成 -1失败',
  ADD COLUMN retry_count INT DEFAULT 0 COMMENT '深度优化重试次数',
  ADD COLUMN partial_response MEDIUMTEXT COMMENT '深度优化中间结果（断点续传）';
