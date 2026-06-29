package com.firesafety.platform.organization;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
public class OrganizationController {
    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping("/enterprises")
    public ApiResponse<List<EnterpriseResponse>> enterprises(
            @AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.listEnterprises(principal).stream().map(EnterpriseResponse::from).toList());
    }

    @PostMapping("/enterprises")
    public ApiResponse<EnterpriseCreationResponse> createEnterprise(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody CreateEnterpriseRequest request) {
        var result = service.createEnterprise(
                principal,
                request.parentId(),
                request.name(),
                request.contactName(),
                request.contactPhone(),
                request.administratorName(),
                request.administratorPhone());
        return ApiResponse.ok(new EnterpriseCreationResponse(
                EnterpriseResponse.from(result.enterprise()), UserResponse.from(result.administrator())));
    }

    @GetMapping("/enterprises/{enterpriseId}/users")
    public ApiResponse<List<UserResponse>> users(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long enterpriseId) {
        return ApiResponse.ok(service.listEnterpriseUsers(principal, enterpriseId).stream()
                .map(UserResponse::from).toList());
    }

    @PostMapping("/users/{userId}/disable")
    public ApiResponse<UserResponse> disableUser(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long userId) {
        return ApiResponse.ok(UserResponse.from(service.disableUser(principal, userId)));
    }

    public record CreateEnterpriseRequest(
            Long parentId,
            @NotBlank String name,
            @NotBlank String contactName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String contactPhone,
            @NotBlank String administratorName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String administratorPhone) {}

    public record EnterpriseCreationResponse(EnterpriseResponse enterprise, UserResponse administrator) {}

    public record EnterpriseResponse(
            Long id, Long parentId, String name, String contactName, String contactPhone, boolean enabled) {
        static EnterpriseResponse from(Enterprise value) {
            return new EnterpriseResponse(value.id(), value.parentId(), value.name(), value.contactName(),
                    value.contactPhone(), value.enabled());
        }
    }

    public record UserResponse(
            Long id, Long enterpriseId, String displayName, String phone, UserRole role, boolean enabled,
            boolean weChatBound) {
        static UserResponse from(UserAccount value) {
            return new UserResponse(value.id(), value.enterpriseId(), value.displayName(), value.phone(),
                    value.role(), value.enabled(), value.openid() != null);
        }
    }
}
