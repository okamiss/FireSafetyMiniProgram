package com.firesafety.platform.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageDeliveryServiceTest {
    @Mock private StationMessageRepository messages;
    @Mock private UserAccountRepository users;
    @Mock private WeChatSubscribeMessageClient client;
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void recordsProviderFailureWithoutThrowingIntoBusinessTransaction() {
        var message = StationMessage.restore(10L, 30L, 20L, "REPAIR_STATUS", "报修状态", "已受理",
                "REPAIR", 8L, false, null, ExternalDeliveryStatus.PENDING, null, null, null,
                Instant.parse("2026-06-30T01:00:00Z"));
        var user = UserAccount.employee(30L, "张三", "13800000000", UserRole.EMPLOYEE);
        user.assignId(20L);
        user.bindOpenid("openid-20");
        when(messages.findByIdForUpdate(10L)).thenReturn(Optional.of(message));
        when(users.findById(20L)).thenReturn(Optional.of(user));
        when(client.send(message, user)).thenThrow(new RuntimeException("provider unavailable"));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new MessageDeliveryService(messages, users, client, clock);

        service.dispatch(10L);

        assertThat(message.externalStatus()).isEqualTo(ExternalDeliveryStatus.FAILED);
        assertThat(message.externalErrorCode()).isEqualTo("WECHAT_SEND_FAILED");
    }
}
