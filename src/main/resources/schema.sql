-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- 创建章节表
CREATE TABLE IF NOT EXISTS chapters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    sort_order INT
);

-- 创建题目表
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    question_number VARCHAR(20),
    sort_order INT,
    chapter_id BIGINT,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id)
);

-- 创建答案表
CREATE TABLE IF NOT EXISTS answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(5000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    question_id BIGINT,
    user_id BIGINT,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);