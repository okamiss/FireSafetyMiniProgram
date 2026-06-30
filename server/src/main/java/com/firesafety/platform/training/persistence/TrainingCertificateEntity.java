package com.firesafety.platform.training.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "training_certificate")
class TrainingCertificateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "record_id", nullable = false) Long recordId;
    @Column(name = "task_id", nullable = false) Long taskId;
    @Column(name = "user_id", nullable = false) Long userId;
    @Column(name = "enterprise_id", nullable = false) Long enterpriseId;
    @Column(name = "certificate_no", nullable = false, length = 64) String certificateNo;
    @Column(name = "storage_key", nullable = false, length = 500) String storageKey;
    @Column(name = "issued_at", nullable = false) Instant issuedAt;

    protected TrainingCertificateEntity() {}
}
