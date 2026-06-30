package com.firesafety.platform.training;

import java.util.List;

public record TrainingAttemptResult(
        TrainingRecord record, List<TrainingAnswerDetail> details, List<TrainingQuestion> questions) {
    public TrainingAttemptResult {
        details = List.copyOf(details);
        questions = List.copyOf(questions);
    }

    public TrainingAttemptResult(TrainingRecord record, List<TrainingAnswerDetail> details) {
        this(record, details, List.of());
    }
}
