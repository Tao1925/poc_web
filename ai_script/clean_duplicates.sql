-- 清理重复数据脚本
-- 在 H2 控制台中执行此脚本来清理重复的数据

-- ===========================================
-- 1. 查看重复数据
-- ===========================================
SELECT '=== 检查重复的用户 ===' as info;
SELECT username, COUNT(*) as count 
FROM users 
GROUP BY username 
HAVING COUNT(*) > 1;

SELECT '=== 检查重复的章节 ===' as info;
SELECT title, COUNT(*) as count 
FROM chapters 
GROUP BY title 
HAVING COUNT(*) > 1;

SELECT '=== 检查重复的题目 ===' as info;
SELECT question_number, title, COUNT(*) as count 
FROM questions 
GROUP BY question_number, title 
HAVING COUNT(*) > 1;

-- ===========================================
-- 2. 删除所有数据（如果需要完全重建）
-- ===========================================
-- 取消下面的注释来删除所有数据并重建
/*
DELETE FROM answers;
DELETE FROM questions;
DELETE FROM chapters;
DELETE FROM users;

-- 然后执行 init_database.sql 来重新导入数据
*/

-- ===========================================
-- 3. 只保留每组重复数据中ID最小的记录
-- ===========================================
-- 删除重复的用户（保留ID最小的）

DELETE FROM users 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM users 
    GROUP BY username
);


-- 删除重复的章节（保留ID最小的）

DELETE FROM chapters 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM chapters 
    GROUP BY title, sort_order
);


-- 删除重复的题目（保留ID最小的）

DELETE FROM questions 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM questions 
    GROUP BY question_number, title
);


-- ===========================================
-- 4. 验证清理结果
-- ===========================================
SELECT '=== 用户总数 ===' as info;
SELECT COUNT(*) as count FROM users;

SELECT '=== 章节总数 ===' as info;
SELECT COUNT(*) as count FROM chapters;

SELECT '=== 题目总数 ===' as info;
SELECT COUNT(*) as count FROM questions;

SELECT '=== 答案总数 ===' as info;
SELECT COUNT(*) as count FROM answers;
