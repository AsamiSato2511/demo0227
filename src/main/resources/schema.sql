DROP TABLE IF EXISTS question_attempts;
DROP TABLE IF EXISTS exam_results;
DROP TABLE IF EXISTS todos;
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
    understanding INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_subjects_category UNIQUE (field_name, major_name, minor_name),
    CONSTRAINT chk_subjects_understanding CHECK (understanding BETWEEN 0 AND 100)
);

CREATE TABLE todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    subject_id BIGINT NOT NULL,
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_todos_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
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

INSERT INTO exam_settings (id, exam_date) VALUES (1, DATEADD('DAY', 30, CURRENT_DATE));

INSERT INTO exam_results (taken_on, source, score_total, memo) VALUES
(CURRENT_DATE, '令和7年春', 62, '時間配分の見直し'),
(DATEADD('DAY', -7, CURRENT_DATE), '令和6年秋', 58, 'セキュリティを復習'),
(DATEADD('DAY', -14, CURRENT_DATE), '令和6年春', 65, '基礎理論を改善');
