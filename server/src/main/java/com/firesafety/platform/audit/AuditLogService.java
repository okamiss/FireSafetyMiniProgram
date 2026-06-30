package com.firesafety.platform.audit;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AuditLogService implements AuditLogPort {
    private final OperationLogRepository logs;
    private final Clock clock;

    public AuditLogService(OperationLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @Override
    public OperationLog record(
            SessionPrincipal operator,
            AuditModule module,
            AuditAction action,
            Long businessId,
            String detail,
            String ipAddress) {
        requirePlatformRole(operator);
        return logs.save(OperationLog.success(
                operator, module, action, businessId, detail, ipAddress, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<OperationLog> list(SessionPrincipal principal) {
        requirePlatformRole(principal);
        return logs.findAll().stream()
                .sorted(Comparator.comparing(OperationLog::createdAt).reversed())
                .toList();
    }

    private void requirePlatformRole(SessionPrincipal principal) {
        if (principal == null || (principal.role() != UserRole.SUPER_ADMIN
                && principal.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有操作日志权限", HttpStatus.FORBIDDEN);
        }
    }
}
