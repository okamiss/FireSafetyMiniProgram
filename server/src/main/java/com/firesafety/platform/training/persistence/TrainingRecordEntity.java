package com.firesafety.platform.training.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "training_record")
class TrainingRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "task_id", nullable = false) Long taskId;
    @Column(name = "user_id", nullable = false) Long userId;
    @Column(name = "enterprise_id", nullable = false) Long enterpriseId;
    @Column(nullable = false) int score;
    @Column(nullable = false) boolean passed;
    @Column(name = "attempt_no", nullable = false) int attemptNo;
    @Column(name = "submitted_at", nullable = false) Instant submittedAt;

    protected TrainingRecordEntity() {}
}
