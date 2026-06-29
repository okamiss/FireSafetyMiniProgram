CREATE TABLE enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    type VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(100) NULL,
    contact_phone VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enterprise_parent FOREIGN KEY (parent_id) REFERENCES enterprise (id)
);

CREATE INDEX idx_enterprise_parent ON enterprise (parent_id);
CREATE INDEX idx_enterprise_status ON enterprise (status);

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NULL,
    username VARCHAR(100) NULL,
    password_hash VARCHAR(255) NULL,
    openid VARCHAR(128) NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_openid UNIQUE (openid),
    CONSTRAINT uk_user_phone UNIQUE (phone)
);

CREATE INDEX idx_user_enterprise ON sys_user (enterprise_id);
CREATE INDEX idx_user_role_status ON sys_user (role, status);

CREATE TABLE file_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id),
    CONSTRAINT uk_file_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_file_business ON file_resource (business_type, business_id);

CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NULL,
    operator_id BIGINT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    result VARCHAR(32) NOT NULL,
    detail VARCHAR(1000) NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise (id),
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_log_operator_time ON operation_log (operator_id, created_at);
CREATE INDEX idx_log_business ON operation_log (module, business_id);
