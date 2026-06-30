package com.firesafety.platform.training;

import java.util.Set;

public record TrainingAnswerDetail(
        Long id, Long recordId, Long questionId, Set<String> userAnswers,
        boolean correct, int awardedScore) {
    public TrainingAnswerDetail {
        userAnswers = Set.copyOf(userAnswers);
    }

    public static TrainingAnswerDetail from(Long recordId, TrainingAnswerResult result) {
        return new TrainingAnswerDetail(null, recordId, result.questionId(), result.userAnswers(),
                result.correct(), result.awardedScore());
    }
}
