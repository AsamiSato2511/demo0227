CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    category_id BIGINT NOT NULL DEFAULT 1,
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_todos_category FOREIGN KEY (category_id) REFERENCES category(id)
);

MERGE INTO category (id, name, color) KEY(id) VALUES (1, 'Work', '#dc3545');
MERGE INTO category (id, name, color) KEY(id) VALUES (2, 'Personal', '#198754');
MERGE INTO category (id, name, color) KEY(id) VALUES (3, 'Study', '#ffc107');
