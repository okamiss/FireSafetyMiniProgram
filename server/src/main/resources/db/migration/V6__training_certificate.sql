CREATE TABLE training_certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enterprise_id BIGINT NOT NULL,
    certificate_no VARCHAR(64) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_training_certificate_record FOREIGN KEY (record_id) REFERENCES training_record (id),
    CONSTRAINT fk_training_certificate_task FOREIGN KEY (task_id) REFERENCES training_task (id),
    CONSTRAINT fk_training_certificate_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_training_certificate_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT uk_training_certificate_record UNIQUE (record_id),
    CONSTRAINT uk_training_certificate_task_user UNIQUE (task_id, user_id),
    CONSTRAINT uk_training_certificate_no UNIQUE (certificate_no),
    CONSTRAINT uk_training_certificate_storage UNIQUE (storage_key)
);

CREATE INDEX idx_training_certificate_user ON training_certificate (user_id, issued_at);
