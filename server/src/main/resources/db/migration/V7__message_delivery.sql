ALTER TABLE station_message ADD COLUMN read_at TIMESTAMP NULL;
ALTER TABLE station_message ADD COLUMN external_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';
ALTER TABLE station_message ADD COLUMN external_error_code VARCHAR(64) NULL;
ALTER TABLE station_message ADD COLUMN external_error_message VARCHAR(500) NULL;
ALTER TABLE station_message ADD COLUMN external_sent_at TIMESTAMP NULL;

CREATE INDEX idx_message_external_status ON station_message (external_status, created_at);
