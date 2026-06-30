package com.firesafety.platform.message.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "station_message")
class StationMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "enterprise_id", nullable = false) Long enterpriseId;
    @Column(name = "recipient_user_id", nullable = false) Long recipientUserId;
    @Column(name = "message_type", nullable = false, length = 64) String messageType;
    @Column(nullable = false, length = 200) String title;
    @Column(nullable = false, length = 1000) String content;
    @Column(name = "business_type", length = 64) String businessType;
    @Column(name = "business_id") Long businessId;
    @Column(name = "read_flag", nullable = false) boolean read;
    @Column(name = "read_at") Instant readAt;
    @Column(name = "external_status", nullable = false, length = 32) String externalStatus;
    @Column(name = "external_error_code", length = 64) String externalErrorCode;
    @Column(name = "external_error_message", length = 500) String externalErrorMessage;
    @Column(name = "external_sent_at") Instant externalSentAt;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    protected StationMessageEntity() {}
}
