package com.firesafety.platform.message;

import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class MessageDeliveryService {
    private final StationMessageRepository messages;
    private final UserAccountRepository users;
    private final WeChatSubscribeMessageClient client;
    private final Clock clock;

    public MessageDeliveryService(
            StationMessageRepository messages,
            UserAccountRepository users,
            WeChatSubscribeMessageClient client,
            Clock clock) {
        this.messages = messages;
        this.users = users;
        this.client = client;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Long messageId) {
        var message = messages.findByIdForUpdate(messageId).orElse(null);
        if (message == null || message.externalStatus() != ExternalDeliveryStatus.PENDING) return;
        var recipient = users.findById(message.recipientUserId()).orElse(null);
        DeliveryOutcome outcome;
        if (recipient == null) {
            outcome = DeliveryOutcome.skipped("RECIPIENT_NOT_FOUND", "接收账号不存在");
        } else {
            try {
                outcome = client.send(message, recipient);
            } catch (RuntimeException exception) {
                outcome = DeliveryOutcome.failed("WECHAT_SEND_FAILED", "微信订阅消息发送失败");
            }
        }
        message.recordDelivery(outcome, Instant.now(clock));
        messages.save(message);
    }
}
