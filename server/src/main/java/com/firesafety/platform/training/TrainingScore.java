package com.firesafety.platform.training;

import java.util.List;

public record TrainingScore(int totalScore, List<TrainingAnswerResult> details) {
    public TrainingScore {
        details = List.copyOf(details);
    }
}
