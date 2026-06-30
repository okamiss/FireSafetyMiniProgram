package com.firesafety.platform.audit;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {
    private final AuditLogService service;

    public AuditController(AuditLogService service) { this.service = service; }

    @GetMapping("/api/admin/audit-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<List<OperationLogResponse>> list(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal).stream().map(OperationLogResponse::from).toList());
    }

    public record OperationLogResponse(
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
        static OperationLogResponse from(OperationLog value) {
            return new OperationLogResponse(value.id(), value.enterpriseId(), value.operatorId(), value.module(),
                    value.action(), value.businessId(), value.result(), value.detail(), value.ipAddress(),
                    value.createdAt());
        }
    }
}
