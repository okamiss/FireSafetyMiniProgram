package com.firesafety.platform.message;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StationMessageController {
    private final StationMessageService service;
    private final WeChatSubscribeProperties subscriptions;

    public StationMessageController(StationMessageService service, WeChatSubscribeProperties subscriptions) {
        this.service = service;
        this.subscriptions = subscriptions;
    }

    @GetMapping("/api/miniapp/messages")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<List<MessageResponse>> mine(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.listMine(principal).stream().map(MessageResponse::from).toList());
    }

    @PostMapping("/api/miniapp/messages/{id}/read")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<MessageResponse> read(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        return ApiResponse.ok(MessageResponse.from(service.markRead(principal, id)));
    }

    @GetMapping("/api/admin/messages")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<List<MessageResponse>> all(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.listAll(principal).stream().map(MessageResponse::from).toList());
    }

    @GetMapping("/api/miniapp/messages/subscription-config")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<SubscriptionConfigResponse> subscriptionConfig() {
        return ApiResponse.ok(new SubscriptionConfigResponse(
                subscriptions.enabled(), subscriptions.configuredTemplateIds()));
    }

    public record SubscriptionConfigResponse(boolean enabled, List<String> templateIds) {}
    public record MessageResponse(
            Long id, Long enterpriseId, Long recipientUserId, String messageType, String title, String content,
            String businessType, Long businessId, boolean read, Instant readAt,
            ExternalDeliveryStatus externalStatus, String externalErrorCode, String externalErrorMessage,
            Instant externalSentAt, Instant createdAt) {
        static MessageResponse from(StationMessage value) {
            return new MessageResponse(value.id(), value.enterpriseId(), value.recipientUserId(), value.messageType(),
                    value.title(), value.content(), value.businessType(), value.businessId(), value.read(),
                    value.readAt(), value.externalStatus(), value.externalErrorCode(), value.externalErrorMessage(),
                    value.externalSentAt(), value.createdAt());
        }
    }
}
