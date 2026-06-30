package com.firesafety.platform.training;

import com.firesafety.platform.common.BusinessException;
import java.time.Instant;

public class TrainingParticipant {
    private Long id;
    private final Long taskId;
    private final Long userId;
    private final Long enterpriseId;
    private int attemptsUsed;
    private int bestScore;
    private boolean passed;
    private Instant completedAt;

    private TrainingParticipant(Long taskId, Long userId, Long enterpriseId) {
        this.taskId = taskId;
        this.userId = userId;
        this.enterpriseId = enterpriseId;
    }

    public static TrainingParticipant assigned(Long taskId, Long userId, Long enterpriseId) {
        if (taskId == null || userId == null || enterpriseId == null) {
            throw new IllegalArgumentException("参训任务、用户和企业不能为空");
        }
        return new TrainingParticipant(taskId, userId, enterpriseId);
    }

    public static TrainingParticipant restore(
            Long id, Long taskId, Long userId, Long enterpriseId, int attemptsUsed,
            int bestScore, boolean passed, Instant completedAt) {
        var participant = assigned(taskId, userId, enterpriseId);
        participant.id = id;
        participant.attemptsUsed = attemptsUsed;
        participant.bestScore = bestScore;
        participant.passed = passed;
        participant.completedAt = completedAt;
        return participant;
    }

    public TrainingAttemptDecision recordAttempt(
            int score, int passScore, int maxAttempts, Instant submittedAt) {
        if (attemptsUsed >= maxAttempts) {
            throw new BusinessException("ATTEMPT_LIMIT", "已达到最大作答次数");
        }
        attemptsUsed++;
        bestScore = Math.max(bestScore, score);
        var currentPassed = score >= passScore;
        if (currentPassed && !passed) {
            passed = true;
            completedAt = submittedAt;
        }
        return new TrainingAttemptDecision(attemptsUsed, score, currentPassed);
    }

    public Long id() { return id; }
    public Long taskId() { return taskId; }
    public Long userId() { return userId; }
    public Long enterpriseId() { return enterpriseId; }
    public int attemptsUsed() { return attemptsUsed; }
    public int bestScore() { return bestScore; }
    public boolean passed() { return passed; }
    public Instant completedAt() { return completedAt; }
}
