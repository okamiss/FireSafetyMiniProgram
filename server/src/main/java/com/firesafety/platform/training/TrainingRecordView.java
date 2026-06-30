package com.firesafety.platform.training;

import java.time.Instant;

public record TrainingRecordView(
        Long id,
        Long taskId,
        String taskTitle,
        Long userId,
        String userName,
        Long enterpriseId,
        String enterpriseName,
        int score,
        boolean passed,
        int attemptNo,
        Instant submittedAt) {}
