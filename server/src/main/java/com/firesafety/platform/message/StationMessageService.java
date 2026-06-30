package com.firesafety.platform.message;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class StationMessageService {
    private final StationMessageRepository messages;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public StationMessageService(
            StationMessageRepository messages, ApplicationEventPublisher events, Clock clock) {
        this.messages = messages;
        this.events = events;
        this.clock = clock;
    }

    public StationMessage create(CreateStationMessageCommand command) {
        var saved = messages.save(StationMessage.create(command, Instant.now(clock)));
        events.publishEvent(new StationMessageCreatedEvent(saved.id()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StationMessage> listMine(SessionPrincipal principal) {
        requireAuthenticated(principal);
        return newestFirst(messages.findByRecipientUserId(principal.userId()));
    }

    @Transactional(readOnly = true)
    public List<StationMessage> listAll(SessionPrincipal principal) {
        if (principal == null || (principal.role() != UserRole.SUPER_ADMIN
                && principal.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有消息管理权限", HttpStatus.FORBIDDEN);
        }
        return newestFirst(messages.findAll());
    }

    public StationMessage markRead(SessionPrincipal principal, Long messageId) {
        requireAuthenticated(principal);
        var message = messages.findByIdForUpdate(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "消息不存在", HttpStatus.NOT_FOUND));
        if (!message.recipientUserId().equals(principal.userId())) {
            throw new BusinessException("FORBIDDEN", "没有权限读取该消息", HttpStatus.FORBIDDEN);
        }
        message.markRead(Instant.now(clock));
        return messages.save(message);
    }

    private List<StationMessage> newestFirst(List<StationMessage> values) {
        return values.stream().sorted(Comparator.comparing(StationMessage::createdAt).reversed()).toList();
    }

    private void requireAuthenticated(SessionPrincipal principal) {
        if (principal == null) throw new BusinessException("UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED);
    }
}
