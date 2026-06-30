CREATE TABLE repair_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    reporter_user_id BIGINT NOT NULL,
    urgency VARCHAR(32) NOT NULL,
    fault_type VARCHAR(64) NOT NULL,
    location VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    handler_user_id BIGINT NULL,
    result VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_repair_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT fk_repair_reporter FOREIGN KEY (reporter_user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_repair_handler FOREIGN KEY (handler_user_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_repair_enterprise_status ON repair_ticket (enterprise_id, status, created_at);
CREATE INDEX idx_repair_reporter ON repair_ticket (reporter_user_id, created_at);

CREATE TABLE repair_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repair_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    operator_user_id BIGINT NOT NULL,
    remark VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_repair_history_ticket FOREIGN KEY (repair_id) REFERENCES repair_ticket (id),
    CONSTRAINT fk_repair_history_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_repair_history_ticket ON repair_history (repair_id, created_at);
