package com.firesafety.platform.training;

import java.time.Instant;
import java.util.Set;

public record CreateTrainingTaskCommand(
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        int passScore,
        int maxAttempts,
        Set<Long> questionIds,
        Set<Long> targetEnterpriseIds,
        Set<Long> targetUserIds) {}
