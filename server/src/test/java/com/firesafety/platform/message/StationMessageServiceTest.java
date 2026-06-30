package com.firesafety.platform.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class StationMessageServiceTest {
    @Mock private StationMessageRepository messages;
    @Mock private ApplicationEventPublisher events;
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsMessageAndPublishesAfterCommitDispatchEvent() {
        when(messages.save(any())).thenAnswer(invocation -> {
            var message = (StationMessage) invocation.getArgument(0);
            message.assignId(10L);
            return message;
        });
        var service = new StationMessageService(messages, events, clock);

        var saved = service.create(new CreateStationMessageCommand(
                30L, 20L, "REPAIR_STATUS", "报修状态已更新", "工单已受理", "REPAIR", 9L));

        assertThat(saved.id()).isEqualTo(10L);
        assertThat(saved.externalStatus()).isEqualTo(ExternalDeliveryStatus.PENDING);
        verify(events).publishEvent(new StationMessageCreatedEvent(10L));
    }

    @Test
    void listsOnlyOwnMessagesAndMarksOwnedMessageRead() {
        var message = StationMessage.restore(10L, 30L, 20L, "TRAINING_TASK", "培训任务", "请按时完成",
                "TRAINING", 8L, false, null, ExternalDeliveryStatus.PENDING, null, null, null,
                Instant.parse("2026-06-30T01:00:00Z"));
        when(messages.findByRecipientUserId(20L)).thenReturn(List.of(message));
        when(messages.findByIdForUpdate(10L)).thenReturn(Optional.of(message));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new StationMessageService(messages, events, clock);
        var principal = new SessionPrincipal(20L, UserRole.EMPLOYEE, 30L, "张三");

        assertThat(service.listMine(principal)).containsExactly(message);
        var read = service.markRead(principal, 10L);

        assertThat(read.read()).isTrue();
        assertThat(read.readAt()).isEqualTo(Instant.parse("2026-06-30T02:00:00Z"));
    }

    @Test
    void rejectsReadingAnotherUsersMessage() {
        var message = StationMessage.restore(10L, 30L, 99L, "REPAIR_STATUS", "消息", "内容",
                "REPAIR", 8L, false, null, ExternalDeliveryStatus.PENDING, null, null, null,
                Instant.parse("2026-06-30T01:00:00Z"));
        when(messages.findByIdForUpdate(10L)).thenReturn(Optional.of(message));
        var service = new StationMessageService(messages, events, clock);

        assertThatThrownBy(() -> service.markRead(
                new SessionPrincipal(20L, UserRole.EMPLOYEE, 30L, "张三"), 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("FORBIDDEN"));
    }
}
