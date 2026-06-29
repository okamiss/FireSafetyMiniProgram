ALTER TABLE permission_request ADD COLUMN pending_phone VARCHAR(32) NULL;

UPDATE permission_request
SET pending_phone = requested_phone
WHERE status = 'PENDING';

CREATE UNIQUE INDEX uk_permission_pending_phone ON permission_request (pending_phone);
