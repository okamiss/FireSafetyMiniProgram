CREATE TABLE training_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(1000) NOT NULL,
    options_json VARCHAR(4000) NOT NULL,
    answer_json VARCHAR(1000) NOT NULL,
    score INT NOT NULL,
    category VARCHAR(100) NULL,
    explanation VARCHAR(2000) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_training_question_category ON training_question (category, enabled);

CREATE TABLE training_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    pass_score INT NOT NULL,
    max_attempts INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_training_task_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
);

CREATE INDEX idx_training_task_status_time ON training_task (status, start_at, end_at);

CREATE TABLE training_task_question (
    task_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, question_id),
    CONSTRAINT fk_training_task_question_task FOREIGN KEY (task_id) REFERENCES training_task (id),
    CONSTRAINT fk_training_task_question_question FOREIGN KEY (question_id) REFERENCES training_question (id)
);

CREATE TABLE training_task_target_enterprise (
    task_id BIGINT NOT NULL,
    enterprise_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, enterprise_id),
    CONSTRAINT fk_training_target_enterprise_task FOREIGN KEY (task_id) REFERENCES training_task (id),
    CONSTRAINT fk_training_target_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
);

CREATE TABLE training_task_target_user (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_training_target_user_task FOREIGN KEY (task_id) REFERENCES training_task (id),
    CONSTRAINT fk_training_target_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE TABLE training_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enterprise_id BIGINT NOT NULL,
    attempts_used INT NOT NULL DEFAULT 0,
    best_score INT NOT NULL DEFAULT 0,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_training_participant_task FOREIGN KEY (task_id) REFERENCES training_task (id),
    CONSTRAINT fk_training_participant_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_training_participant_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT uk_training_participant UNIQUE (task_id, user_id)
);

CREATE INDEX idx_training_participant_user ON training_participant (user_id, passed);
CREATE INDEX idx_training_participant_enterprise ON training_participant (enterprise_id, task_id);

CREATE TABLE training_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enterprise_id BIGINT NOT NULL,
    score INT NOT NULL,
    passed BOOLEAN NOT NULL,
    attempt_no INT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_training_record_task FOREIGN KEY (task_id) REFERENCES training_task (id),
    CONSTRAINT fk_training_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_training_record_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT uk_training_record_attempt UNIQUE (task_id, user_id, attempt_no)
);

CREATE INDEX idx_training_record_task ON training_record (task_id, submitted_at);
CREATE INDEX idx_training_record_user ON training_record (user_id, submitted_at);

CREATE TABLE training_answer_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer_json VARCHAR(1000) NOT NULL,
    correct BOOLEAN NOT NULL,
    awarded_score INT NOT NULL,
    CONSTRAINT fk_training_answer_record FOREIGN KEY (record_id) REFERENCES training_record (id),
    CONSTRAINT fk_training_answer_question FOREIGN KEY (question_id) REFERENCES training_question (id),
    CONSTRAINT uk_training_answer_question UNIQUE (record_id, question_id)
);

CREATE INDEX idx_training_answer_record ON training_answer_detail (record_id);
