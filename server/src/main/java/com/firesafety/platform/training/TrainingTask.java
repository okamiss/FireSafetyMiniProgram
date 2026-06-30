package com.firesafety.platform.training;

import com.firesafety.platform.common.BusinessException;
import java.time.Instant;
import java.util.Set;

public class TrainingTask {
    private final Long id;
    private final String title;
    private final String description;
    private final Instant startAt;
    private final Instant endAt;
    private final int passScore;
    private final int maxAttempts;
    private TrainingTaskStatus status;
    private final Set<Long> questionIds;
    private final Set<Long> targetEnterpriseIds;
    private final Set<Long> targetUserIds;
    private final Long createdBy;
    private final Instant createdAt;
    private Instant publishedAt;

    private TrainingTask(
            Long id, String title, String description, Instant startAt, Instant endAt,
            int passScore, int maxAttempts, TrainingTaskStatus status, Set<Long> questionIds,
            Set<Long> targetEnterpriseIds, Set<Long> targetUserIds, Long createdBy,
            Instant createdAt, Instant publishedAt) {
        if (title == null || title.isBlank() || startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("任务标题和有效作答时间不能为空");
        }
        if (passScore <= 0 || maxAttempts <= 0) {
            throw new IllegalArgumentException("及格分和最大作答次数必须大于零");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.passScore = passScore;
        this.maxAttempts = maxAttempts;
        this.status = status;
        this.questionIds = Set.copyOf(questionIds);
        this.targetEnterpriseIds = Set.copyOf(targetEnterpriseIds);
        this.targetUserIds = Set.copyOf(targetUserIds);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public static TrainingTask restore(
            Long id, String title, String description, Instant startAt, Instant endAt,
            int passScore, int maxAttempts, TrainingTaskStatus status, Set<Long> questionIds,
            Set<Long> targetEnterpriseIds, Set<Long> targetUserIds, Long createdBy,
            Instant createdAt, Instant publishedAt) {
        return new TrainingTask(id, title, description, startAt, endAt, passScore, maxAttempts,
                status, questionIds, targetEnterpriseIds, targetUserIds, createdBy, createdAt, publishedAt);
    }

    public static TrainingTask draft(CreateTrainingTaskCommand command, Long createdBy, Instant createdAt) {
        return new TrainingTask(null, command.title(), command.description(), command.startAt(), command.endAt(),
                command.passScore(), command.maxAttempts(), TrainingTaskStatus.DRAFT, command.questionIds(),
                command.targetEnterpriseIds(), command.targetUserIds(), createdBy, createdAt, null);
    }

    public void publish(Instant now, int totalQuestionScore) {
        if (status != TrainingTaskStatus.DRAFT) {
            throw new BusinessException("INVALID_TASK_STATUS", "只有草稿任务可以发布");
        }
        if (questionIds.isEmpty()) {
            throw new BusinessException("TASK_QUESTIONS_REQUIRED", "培训任务至少需要一道题目");
        }
        if (targetEnterpriseIds.isEmpty() && targetUserIds.isEmpty()) {
            throw new BusinessException("TASK_TARGETS_REQUIRED", "培训任务必须设置参训对象");
        }
        if (totalQuestionScore < passScore) {
            throw new BusinessException("INSUFFICIENT_TASK_SCORE", "题目总分不能低于及格分");
        }
        if (!now.isBefore(endAt)) {
            throw new BusinessException("TASK_ALREADY_EXPIRED", "培训任务已过截止时间");
        }
        status = TrainingTaskStatus.PUBLISHED;
        publishedAt = now;
    }

    public Long id() { return id; }
    public String title() { return title; }
    public String description() { return description; }
    public Instant startAt() { return startAt; }
    public Instant endAt() { return endAt; }
    public int passScore() { return passScore; }
    public int maxAttempts() { return maxAttempts; }
    public TrainingTaskStatus status() { return status; }
    public Set<Long> questionIds() { return questionIds; }
    public Set<Long> targetEnterpriseIds() { return targetEnterpriseIds; }
    public Set<Long> targetUserIds() { return targetUserIds; }
    public Long createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
    public Instant publishedAt() { return publishedAt; }
}
