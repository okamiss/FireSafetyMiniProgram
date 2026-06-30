package com.firesafety.platform.training;

import java.util.List;

public record TrainingPaper(
        TrainingTask task, TrainingParticipant participant, List<TrainingQuestion> questions) {
    public TrainingPaper {
        questions = List.copyOf(questions);
    }
}
