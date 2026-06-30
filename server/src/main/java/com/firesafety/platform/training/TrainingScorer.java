package com.firesafety.platform.training;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class TrainingScorer {
    public TrainingScore score(
            Iterable<TrainingQuestion> questions, Map<Long, Set<String>> submittedAnswers) {
        var total = 0;
        var details = new ArrayList<TrainingAnswerResult>();
        for (var question : questions) {
            var answer = submittedAnswers.getOrDefault(question.id(), Set.of());
            var correct = question.correctAnswers().equals(answer);
            var awarded = correct ? question.score() : 0;
            total += awarded;
            details.add(new TrainingAnswerResult(question.id(), answer, correct, awarded));
        }
        return new TrainingScore(total, details);
    }
}
