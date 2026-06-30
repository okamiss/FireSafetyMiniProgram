package com.firesafety.platform.permission;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionRequestController {
    private final PermissionRequestService service;

    public PermissionRequestController(PermissionRequestService service) {
        this.service = service;
    }

    @PostMapping("/api/miniapp/permission-requests")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    public ApiResponse<PermissionRequestResponse> requestEmployee(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody EmployeePermissionRequest request) {
        return ApiResponse.ok(PermissionRequestResponse.from(
                service.requestEmployee(principal, request.name(), request.phone())));
    }

    @GetMapping("/api/miniapp/permission-requests")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    public ApiResponse<java.util.List<PermissionRequestResponse>> miniappRequests(
            @AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal).stream().map(PermissionRequestResponse::from).toList());
    }

    @GetMapping("/api/admin/permission-requests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<java.util.List<PermissionRequestResponse>> adminRequests(
            @AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal).stream().map(PermissionRequestResponse::from).toList());
    }

    @PostMapping("/api/admin/permission-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<PermissionRequestResponse> approve(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(PermissionRequestResponse.from(
                service.approve(principal, id, request.remark(), httpRequest.getRemoteAddr())));
    }

    @PostMapping("/api/admin/permission-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<PermissionRequestResponse> reject(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(PermissionRequestResponse.from(
                service.reject(principal, id, request.remark(), httpRequest.getRemoteAddr())));
    }

    public record EmployeePermissionRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone) {}

    public record ReviewRequest(@NotBlank String remark) {}

    public record PermissionRequestResponse(
            Long id,
            Long enterpriseId,
            Long applicantUserId,
            String requestedName,
            String requestedPhone,
            PermissionRequestStatus status,
            Long reviewerUserId,
            String reviewRemark,
            Instant createdAt,
            Instant reviewedAt) {
        static PermissionRequestResponse from(PermissionRequest request) {
            return new PermissionRequestResponse(
                    request.id(), request.enterpriseId(), request.applicantUserId(), request.requestedName(),
                    request.requestedPhone(), request.status(), request.reviewerUserId(), request.reviewRemark(),
                    request.createdAt(), request.reviewedAt());
        }
    }
}
