package com.firesafety.platform.training;

import java.time.Instant;

public class TrainingRecord {
    private Long id;
    private final Long taskId;
    private final Long userId;
    private final Long enterpriseId;
    private final int score;
    private final boolean passed;
    private final int attemptNo;
    private final Instant submittedAt;

    private TrainingRecord(
            Long taskId, Long userId, Long enterpriseId, int score,
            boolean passed, int attemptNo, Instant submittedAt) {
        this.taskId = taskId;
        this.userId = userId;
        this.enterpriseId = enterpriseId;
        this.score = score;
        this.passed = passed;
        this.attemptNo = attemptNo;
        this.submittedAt = submittedAt;
    }

    public static TrainingRecord create(
            Long taskId, Long userId, Long enterpriseId, TrainingAttemptDecision decision, Instant submittedAt) {
        return new TrainingRecord(taskId, userId, enterpriseId, decision.score(),
                decision.passed(), decision.attemptNo(), submittedAt);
    }

    public static TrainingRecord restore(
            Long id, Long taskId, Long userId, Long enterpriseId, int score,
            boolean passed, int attemptNo, Instant submittedAt) {
        var record = new TrainingRecord(
                taskId, userId, enterpriseId, score, passed, attemptNo, submittedAt);
        record.id = id;
        return record;
    }

    public void assignId(Long id) {
        if (this.id != null) throw new IllegalStateException("培训记录编号已经分配");
        this.id = id;
    }

    public Long id() { return id; }
    public Long taskId() { return taskId; }
    public Long userId() { return userId; }
    public Long enterpriseId() { return enterpriseId; }
    public int score() { return score; }
    public boolean passed() { return passed; }
    public int attemptNo() { return attemptNo; }
    public Instant submittedAt() { return submittedAt; }
}
