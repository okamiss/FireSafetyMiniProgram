package com.firesafety.platform.training.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "training_task")
class TrainingTaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 200) String title;
    @Column(length = 2000) String description;
    @Column(name = "start_at", nullable = false) Instant startAt;
    @Column(name = "end_at", nullable = false) Instant endAt;
    @Column(name = "pass_score", nullable = false) int passScore;
    @Column(name = "max_attempts", nullable = false) int maxAttempts;
    @Column(nullable = false, length = 32) String status;
    @Column(name = "created_by", nullable = false) Long createdBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "published_at") Instant publishedAt;
    @Version long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "training_task_question", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "question_id")
    Set<Long> questionIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "training_task_target_enterprise", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "enterprise_id")
    Set<Long> targetEnterpriseIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "training_task_target_user", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "user_id")
    Set<Long> targetUserIds = new HashSet<>();

    protected TrainingTaskEntity() {}
}
