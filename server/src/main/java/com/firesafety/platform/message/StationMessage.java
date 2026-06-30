package com.firesafety.platform.message;

import java.time.Instant;

public class StationMessage {
    private Long id;
    private final Long enterpriseId;
    private final Long recipientUserId;
    private final String messageType;
    private final String title;
    private final String content;
    private final String businessType;
    private final Long businessId;
    private boolean read;
    private Instant readAt;
    private ExternalDeliveryStatus externalStatus;
    private String externalErrorCode;
    private String externalErrorMessage;
    private Instant externalSentAt;
    private final Instant createdAt;

    private StationMessage(
            Long id, Long enterpriseId, Long recipientUserId, String messageType, String title, String content,
            String businessType, Long businessId, boolean read, Instant readAt,
            ExternalDeliveryStatus externalStatus, String externalErrorCode, String externalErrorMessage,
            Instant externalSentAt, Instant createdAt) {
        this.id = id;
        this.enterpriseId = enterpriseId;
        this.recipientUserId = recipientUserId;
        this.messageType = messageType;
        this.title = title;
        this.content = content;
        this.businessType = businessType;
        this.businessId = businessId;
        this.read = read;
        this.readAt = readAt;
        this.externalStatus = externalStatus;
        this.externalErrorCode = externalErrorCode;
        this.externalErrorMessage = externalErrorMessage;
        this.externalSentAt = externalSentAt;
        this.createdAt = createdAt;
    }

    public static StationMessage create(CreateStationMessageCommand command, Instant createdAt) {
        return new StationMessage(null, command.enterpriseId(), command.recipientUserId(), command.messageType(),
                command.title(), command.content(), command.businessType(), command.businessId(), false, null,
                ExternalDeliveryStatus.PENDING, null, null, null, createdAt);
    }

    public static StationMessage restore(
            Long id, Long enterpriseId, Long recipientUserId, String messageType, String title, String content,
            String businessType, Long businessId, boolean read, Instant readAt,
            ExternalDeliveryStatus externalStatus, String externalErrorCode, String externalErrorMessage,
            Instant externalSentAt, Instant createdAt) {
        return new StationMessage(id, enterpriseId, recipientUserId, messageType, title, content, businessType,
                businessId, read, readAt, externalStatus, externalErrorCode, externalErrorMessage,
                externalSentAt, createdAt);
    }

    public void assignId(Long id) {
        if (this.id != null) throw new IllegalStateException("消息编号已经分配");
        this.id = id;
    }

    public void markRead(Instant now) {
        if (!read) { read = true; readAt = now; }
    }

    public void recordDelivery(DeliveryOutcome outcome, Instant now) {
        externalStatus = outcome.status();
        externalErrorCode = outcome.errorCode();
        externalErrorMessage = outcome.errorMessage();
        externalSentAt = outcome.status() == ExternalDeliveryStatus.SENT ? now : null;
    }

    public Long id() { return id; }
    public Long enterpriseId() { return enterpriseId; }
    public Long recipientUserId() { return recipientUserId; }
    public String messageType() { return messageType; }
    public String title() { return title; }
    public String content() { return content; }
    public String businessType() { return businessType; }
    public Long businessId() { return businessId; }
    public boolean read() { return read; }
    public Instant readAt() { return readAt; }
    public ExternalDeliveryStatus externalStatus() { return externalStatus; }
    public String externalErrorCode() { return externalErrorCode; }
    public String externalErrorMessage() { return externalErrorMessage; }
    public Instant externalSentAt() { return externalSentAt; }
    public Instant createdAt() { return createdAt; }
}
