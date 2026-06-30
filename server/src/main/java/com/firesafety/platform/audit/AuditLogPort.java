package com.firesafety.platform.audit;

import com.firesafety.platform.auth.SessionPrincipal;

public interface AuditLogPort {
    OperationLog record(
            SessionPrincipal operator,
            AuditModule module,
            AuditAction action,
            Long businessId,
            String detail,
            String ipAddress);
}
