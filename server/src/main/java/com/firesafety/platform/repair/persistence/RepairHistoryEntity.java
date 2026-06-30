package com.firesafety.platform.repair.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repair_history")
class RepairHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "repair_id", nullable = false) Long repairId;
    @Column(name = "from_status", length = 32) String fromStatus;
    @Column(name = "to_status", nullable = false, length = 32) String toStatus;
    @Column(name = "operator_user_id", nullable = false) Long operatorUserId;
    @Column(nullable = false, length = 1000) String remark;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    protected RepairHistoryEntity() {}
}
