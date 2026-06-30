package com.firesafety.platform.audit.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "operation_log")
class OperationLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "enterprise_id") Long enterpriseId;
    @Column(name = "operator_id") Long operatorId;
    @Column(nullable = false, length = 64) String module;
    @Column(nullable = false, length = 64) String action;
    @Column(name = "business_id") Long businessId;
    @Column(nullable = false, length = 32) String result;
    @Column(length = 1000) String detail;
    @Column(name = "ip_address", length = 64) String ipAddress;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    protected OperationLogEntity() {}
}
