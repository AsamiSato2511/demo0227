DROP TABLE IF EXISTS question_attempts;
DROP TABLE IF EXISTS exam_results;
DROP TABLE IF EXISTS words;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS exam_settings;

CREATE TABLE exam_settings (
    id BIGINT PRIMARY KEY,
    exam_date DATE NOT NULL
);

CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name VARCHAR(100) NOT NULL,
    major_name VARCHAR(100) NOT NULL,
    minor_name VARCHAR(150) NOT NULL,
    color VARCHAR(20) NOT NULL,
    CONSTRAINT uq_subjects_category UNIQUE (field_name, major_name, minor_name)
);

CREATE TABLE exam_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taken_on DATE NOT NULL,
    source VARCHAR(100) NOT NULL,
    score_total INT NOT NULL,
    memo VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exam_results_score CHECK (score_total BETWEEN 0 AND 100)
);

CREATE TABLE question_attempts (
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

CREATE TABLE words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term VARCHAR(200) NOT NULL,
    meaning VARCHAR(1000) NOT NULL,
    field_name VARCHAR(100) NOT NULL DEFAULT '未分類',
    major_name VARCHAR(100) NOT NULL DEFAULT '未分類',
    minor_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT '学習中',
    wrong_count INT NOT NULL DEFAULT 0,
    last_reviewed_on DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_words_term UNIQUE (term)
);

INSERT INTO exam_settings (id, exam_date) VALUES (1, DATEADD('DAY', 45, CURRENT_DATE));

INSERT INTO subjects (field_name, major_name, minor_name, color) VALUES
('Technology', 'Database', 'Relational DB', '#0d6efd'),
('Technology', 'Security', 'Network Security', '#198754'),
('Strategy', 'Management', 'Finance', '#fd7e14');

INSERT INTO exam_results (taken_on, source, score_total, memo) VALUES
(DATEADD('DAY', -14, CURRENT_DATE), '模試A', 58, '午後問題で失点'),
(DATEADD('DAY', -7, CURRENT_DATE), '模試B', 63, 'セキュリティ改善'),
(CURRENT_DATE, '模試C', 67, 'あと3点');

INSERT INTO question_attempts (attempted_on, correct, field_name, major_name, minor_name, source_label, source_url, import_batch_id) VALUES
(DATEADD('DAY', -6, CURRENT_DATE), FALSE, 'Technology', 'Security', 'Network Security', 'Q1', 'https://example.com/q1', 'seed-batch'),
(DATEADD('DAY', -5, CURRENT_DATE), FALSE, 'Technology', 'Security', 'Network Security', 'Q2', 'https://example.com/q2', 'seed-batch'),
(DATEADD('DAY', -4, CURRENT_DATE), TRUE,  'Technology', 'Security', 'Network Security', 'Q3', 'https://example.com/q3', 'seed-batch'),
(DATEADD('DAY', -3, CURRENT_DATE), FALSE, 'Technology', 'Database', 'Relational DB', 'Q4', 'https://example.com/q4', 'seed-batch'),
(DATEADD('DAY', -2, CURRENT_DATE), TRUE,  'Technology', 'Database', 'Relational DB', 'Q5', 'https://example.com/q5', 'seed-batch'),
(DATEADD('DAY', -1, CURRENT_DATE), FALSE, 'Strategy', 'Management', 'Finance', 'Q6', 'https://example.com/q6', 'seed-batch'),
(DATEADD('DAY', -6, CURRENT_DATE), FALSE, 'Strategy', 'Management', 'Finance', 'Q7', 'https://example.com/q7', 'seed-batch'),
(DATEADD('DAY', -5, CURRENT_DATE), TRUE,  'Strategy', 'Management', 'Finance', 'Q8', 'https://example.com/q8', 'seed-batch'),
(DATEADD('DAY', -4, CURRENT_DATE), FALSE, 'Strategy', 'Management', 'Finance', 'Q9', 'https://example.com/q9', 'seed-batch'),
(DATEADD('DAY', -3, CURRENT_DATE), FALSE, 'Strategy', 'Management', 'Finance', 'Q10', 'https://example.com/q10', 'seed-batch');

INSERT INTO words (term, meaning, minor_name, status, wrong_count) VALUES
('CAPEX', '設備投資。長期資産への投資。', 'Finance', '学習中', 1),
('PKI', '公開鍵基盤。電子証明書を扱う仕組み。', 'Network Security', '要復習', 3),
('Primary Key', 'テーブルで行を一意に識別するキー。', 'Relational DB', '学習中', 2);
