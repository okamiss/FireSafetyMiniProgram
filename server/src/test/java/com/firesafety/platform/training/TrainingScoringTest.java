package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrainingScoringTest {
    private final TrainingScorer scorer = new TrainingScorer();

    @Test
    void awardsPointsOnlyForExactAnswersAcrossSupportedQuestionTypes() {
        var questions = List.of(
                question(1L, QuestionType.SINGLE_CHOICE, Set.of("B"), 30),
                question(2L, QuestionType.MULTIPLE_CHOICE, Set.of("A", "C"), 40),
                question(3L, QuestionType.TRUE_FALSE, Set.of("TRUE"), 30));

        var result = scorer.score(questions, Map.of(
                1L, Set.of("B"),
                2L, Set.of("A", "C"),
                3L, Set.of("FALSE")));

        assertThat(result.totalScore()).isEqualTo(70);
        assertThat(result.details()).extracting(TrainingAnswerResult::awardedScore)
                .containsExactly(30, 40, 0);
        assertThat(result.details()).extracting(TrainingAnswerResult::correct)
                .containsExactly(true, true, false);
    }

    @Test
    void givesZeroForPartialMultipleChoiceAndUnansweredQuestions() {
        var questions = List.of(
                question(1L, QuestionType.MULTIPLE_CHOICE, Set.of("A", "C"), 50),
                question(2L, QuestionType.SINGLE_CHOICE, Set.of("B"), 50));

        var result = scorer.score(questions, Map.of(1L, Set.of("A")));

        assertThat(result.totalScore()).isZero();
        assertThat(result.details()).allMatch(detail -> !detail.correct() && detail.awardedScore() == 0);
    }

    private TrainingQuestion question(Long id, QuestionType type, Set<String> answer, int score) {
        var options = new LinkedHashMap<String, String>();
        options.put("A", "选项 A");
        options.put("B", "选项 B");
        options.put("C", "选项 C");
        if (type == QuestionType.TRUE_FALSE) {
            options.clear();
            options.put("TRUE", "正确");
            options.put("FALSE", "错误");
        }
        return TrainingQuestion.restore(id, type, "测试题目", options, answer, score,
                "消防常识", "答案解析", true);
    }
}
