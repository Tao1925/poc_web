-- 验证 answers.content 字段类型
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'ANSWERS' AND COLUMN_NAME = 'CONTENT';

-- 查看所有答案记录
SELECT id, 
       LENGTH(content) as content_length,
       SUBSTRING(content, 1, 100) as content_preview
FROM answers;

