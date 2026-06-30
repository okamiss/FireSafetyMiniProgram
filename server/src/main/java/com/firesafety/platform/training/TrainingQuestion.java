package com.firesafety.platform.training;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record TrainingQuestion(
        Long id,
        QuestionType type,
        String title,
        Map<String, String> options,
        Set<String> correctAnswers,
        int score,
        String category,
        String explanation,
        boolean enabled) {

    public TrainingQuestion {
        if (type == null || title == null || title.isBlank() || score <= 0 || score > 1000) {
            throw new IllegalArgumentException("题型、题干和分值不能为空");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("题目分类不能为空");
        }
        options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
        correctAnswers = Set.copyOf(correctAnswers);
        if (options.size() < 2) {
            throw new IllegalArgumentException("选择题至少需要两个选项");
        }
        if (correctAnswers.isEmpty() || !options.keySet().containsAll(correctAnswers)) {
            throw new IllegalArgumentException("正确答案必须属于题目选项");
        }
        if (type == QuestionType.MULTIPLE_CHOICE && correctAnswers.size() < 2) {
            throw new IllegalArgumentException("多选题至少需要两个正确答案");
        }
        if (type != QuestionType.MULTIPLE_CHOICE && correctAnswers.size() != 1) {
            throw new IllegalArgumentException("单选题和判断题只能有一个正确答案");
        }
    }

    public static TrainingQuestion restore(
            Long id,
            QuestionType type,
            String title,
            Map<String, String> options,
            Set<String> correctAnswers,
            int score,
            String category,
            String explanation,
            boolean enabled) {
        return new TrainingQuestion(id, type, title, options, correctAnswers, score,
                category, explanation, enabled);
    }
}
