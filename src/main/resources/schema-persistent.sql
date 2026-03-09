CREATE TABLE IF NOT EXISTS exam_settings (
    id BIGINT PRIMARY KEY,
    exam_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name VARCHAR(100) NOT NULL,
    major_name VARCHAR(100) NOT NULL,
    minor_name VARCHAR(150) NOT NULL,
    color VARCHAR(20) NOT NULL,
    CONSTRAINT uq_subjects_category UNIQUE (field_name, major_name, minor_name)
);

CREATE TABLE IF NOT EXISTS exam_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taken_on DATE NOT NULL,
    source VARCHAR(100) NOT NULL,
    score_total INT NOT NULL,
    memo VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exam_results_score CHECK (score_total BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS question_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempted_on DATE NOT NULL,
    correct BOOLEAN NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    major_name VARCHAR(100) NOT NULL,
    minor_name VARCHAR(150) NOT NULL,
    source_label VARCHAR(200),
    source_url VARCHAR(500),
    import_batch_id VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term VARCHAR(200) NOT NULL,
    meaning VARCHAR(1000) NOT NULL,
    field_name VARCHAR(100) NOT NULL DEFAULT '未分類',
    major_name VARCHAR(100) NOT NULL DEFAULT '未分類',
    minor_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT '学習中',
    wrong_count INT NOT NULL DEFAULT 0,
    quiz_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    choice1 VARCHAR(1000),
    choice2 VARCHAR(1000),
    choice3 VARCHAR(1000),
    choice4 VARCHAR(1000),
    answer_index INT,
    last_reviewed_on DATE,
    last_reviewed_at TIMESTAMP,
    last_correct_at TIMESTAMP,
    priority INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_words_term UNIQUE (term)
);

ALTER TABLE words ADD COLUMN IF NOT EXISTS last_reviewed_at TIMESTAMP;
ALTER TABLE words ADD COLUMN IF NOT EXISTS last_correct_at TIMESTAMP;
ALTER TABLE words ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;
ALTER TABLE words ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS learning_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(40) NOT NULL,
    word_id BIGINT,
    answered_at TIMESTAMP NOT NULL,
    correct BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
