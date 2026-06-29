ALTER TABLE enterprise ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE permission_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    requested_name VARCHAR(100) NOT NULL,
    requested_phone VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reviewer_user_id BIGINT NULL,
    review_remark VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_permission_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT fk_permission_applicant FOREIGN KEY (applicant_user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_permission_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_permission_enterprise_status ON permission_request (enterprise_id, status);
CREATE INDEX idx_permission_created_at ON permission_request (created_at);

CREATE TABLE station_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    message_type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    business_type VARCHAR(64) NULL,
    business_id BIGINT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT fk_message_recipient FOREIGN KEY (recipient_user_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_message_recipient_read ON station_message (recipient_user_id, read_flag, created_at);
