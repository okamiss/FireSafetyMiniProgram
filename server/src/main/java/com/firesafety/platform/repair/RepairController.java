package com.firesafety.platform.repair;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepairController {
    private final RepairService service;

    public RepairController(RepairService service) { this.service = service; }

    @PostMapping("/api/miniapp/repairs")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<RepairResponse> create(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody CreateRepairRequest request) {
        return ApiResponse.ok(RepairResponse.from(service.create(principal, request.toCommand())));
    }

    @GetMapping("/api/miniapp/repairs")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<List<RepairResponse>> miniappList(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal).stream().map(RepairResponse::from).toList());
    }

    @GetMapping("/api/admin/repairs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<List<RepairResponse>> adminList(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal).stream().map(RepairResponse::from).toList());
    }

    @GetMapping({"/api/miniapp/repairs/{id}", "/api/admin/repairs/{id}"})
    public ApiResponse<RepairResponse> detail(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        return ApiResponse.ok(RepairResponse.from(service.detail(principal, id)));
    }

    @GetMapping({"/api/miniapp/repairs/{id}/history", "/api/admin/repairs/{id}/history"})
    public ApiResponse<List<RepairHistoryResponse>> history(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        return ApiResponse.ok(service.history(principal, id).stream().map(RepairHistoryResponse::from).toList());
    }

    @PostMapping("/api/admin/repairs/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<RepairResponse> accept(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody RemarkRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(RepairResponse.from(
                service.accept(principal, id, request.remark(), httpRequest.getRemoteAddr())));
    }

    @PostMapping("/api/admin/repairs/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<RepairResponse> complete(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody RemarkRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(RepairResponse.from(
                service.complete(principal, id, request.remark(), httpRequest.getRemoteAddr())));
    }

    @PostMapping("/api/admin/repairs/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<RepairResponse> close(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody RemarkRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(RepairResponse.from(
                service.close(principal, id, request.remark(), httpRequest.getRemoteAddr())));
    }

    public record CreateRepairRequest(
            @NotNull RepairUrgency urgency,
            @NotBlank @Size(max = 64) String faultType,
            @NotBlank @Size(max = 200) String location,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 100) String contactName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String contactPhone) {
        CreateRepairCommand toCommand() {
            return new CreateRepairCommand(urgency, faultType, location, description, contactName, contactPhone);
        }
    }

    public record RemarkRequest(@NotBlank @Size(max = 1000) String remark) {}

    public record RepairResponse(
            Long id, Long enterpriseId, Long reporterUserId, RepairUrgency urgency, String faultType,
            String location, String description, String contactName, String contactPhone, RepairStatus status,
            Long handlerUserId, String result, Instant createdAt, Instant updatedAt,
            Instant completedAt, Instant closedAt) {
        static RepairResponse from(RepairTicket value) {
            return new RepairResponse(value.id(), value.enterpriseId(), value.reporterUserId(), value.urgency(),
                    value.faultType(), value.location(), value.description(), value.contactName(), value.contactPhone(),
                    value.status(), value.handlerUserId(), value.result(), value.createdAt(), value.updatedAt(),
                    value.completedAt(), value.closedAt());
        }
    }

    public record RepairHistoryResponse(
            Long id, RepairStatus fromStatus, RepairStatus toStatus, Long operatorUserId,
            String remark, Instant createdAt) {
        static RepairHistoryResponse from(RepairHistory value) {
            return new RepairHistoryResponse(value.id(), value.fromStatus(), value.toStatus(),
                    value.operatorUserId(), value.remark(), value.createdAt());
        }
    }
}
