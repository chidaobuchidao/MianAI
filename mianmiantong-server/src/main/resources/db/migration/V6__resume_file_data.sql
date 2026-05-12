-- V6__resume_file_data.sql - 保存原始上传文件
ALTER TABLE resume
    ADD COLUMN file_data LONGBLOB COMMENT '原始上传文件二进制数据';
