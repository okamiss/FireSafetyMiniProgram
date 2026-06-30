package com.firesafety.platform.message;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class StationMessageEventListener {
    private final MessageDeliveryService delivery;

    public StationMessageEventListener(MessageDeliveryService delivery) { this.delivery = delivery; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(StationMessageCreatedEvent event) {
        delivery.dispatch(event.messageId());
    }
}
