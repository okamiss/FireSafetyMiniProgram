package com.firesafety.platform.audit;

import com.firesafety.platform.auth.SessionPrincipal;
import java.time.Instant;

public record OperationLog(
        Long id,
        Long enterpriseId,
        Long operatorId,
        AuditModule module,
        AuditAction action,
        Long businessId,
        AuditResult result,
        String detail,
        String ipAddress,
        Instant createdAt) {

    public static OperationLog success(
            SessionPrincipal operator,
            AuditModule module,
            AuditAction action,
            Long businessId,
            String detail,
            String ipAddress,
            Instant createdAt) {
        if (operator == null || module == null || action == null || createdAt == null) {
            throw new IllegalArgumentException("操作日志必要信息不能为空");
        }
        return new OperationLog(null, operator.enterpriseId(), operator.userId(), module, action,
                businessId, AuditResult.SUCCESS, normalize(detail, 1000), normalize(ipAddress, 64), createdAt);
    }

    public OperationLog withId(Long value) {
        return new OperationLog(value, enterpriseId, operatorId, module, action, businessId,
                result, detail, ipAddress, createdAt);
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        var normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
