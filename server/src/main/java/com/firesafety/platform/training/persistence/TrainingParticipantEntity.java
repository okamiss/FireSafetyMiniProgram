package com.firesafety.platform.training.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "training_participant")
class TrainingParticipantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "task_id", nullable = false) Long taskId;
    @Column(name = "user_id", nullable = false) Long userId;
    @Column(name = "enterprise_id", nullable = false) Long enterpriseId;
    @Column(name = "attempts_used", nullable = false) int attemptsUsed;
    @Column(name = "best_score", nullable = false) int bestScore;
    @Column(nullable = false) boolean passed;
    @Column(name = "completed_at") Instant completedAt;
    @Version long version;

    protected TrainingParticipantEntity() {}
}
