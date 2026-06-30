package com.firesafety.platform.training;

import java.util.Map;
import java.util.Set;

public record CreateTrainingQuestionCommand(
        QuestionType type,
        String title,
        Map<String, String> options,
        Set<String> correctAnswers,
        int score,
        String category,
        String explanation) {}
