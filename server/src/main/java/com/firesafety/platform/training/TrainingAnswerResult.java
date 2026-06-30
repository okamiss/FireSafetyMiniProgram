package com.firesafety.platform.training;

import java.util.Set;

public record TrainingAnswerResult(Long questionId, Set<String> userAnswers, boolean correct, int awardedScore) {
    public TrainingAnswerResult {
        userAnswers = Set.copyOf(userAnswers);
    }
}
