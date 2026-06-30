package com.firesafety.platform.repair.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "repair_ticket")
class RepairTicketEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "enterprise_id", nullable = false) Long enterpriseId;
    @Column(name = "reporter_user_id", nullable = false) Long reporterUserId;
    @Column(nullable = false, length = 32) String urgency;
    @Column(name = "fault_type", nullable = false, length = 64) String faultType;
    @Column(nullable = false, length = 200) String location;
    @Column(nullable = false, length = 2000) String description;
    @Column(name = "contact_name", nullable = false, length = 100) String contactName;
    @Column(name = "contact_phone", nullable = false, length = 32) String contactPhone;
    @Column(nullable = false, length = 32) String status;
    @Column(name = "handler_user_id") Long handlerUserId;
    @Column(length = 2000) String result;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "completed_at") Instant completedAt;
    @Column(name = "closed_at") Instant closedAt;
    @Version long version;

    protected RepairTicketEntity() {}
}
