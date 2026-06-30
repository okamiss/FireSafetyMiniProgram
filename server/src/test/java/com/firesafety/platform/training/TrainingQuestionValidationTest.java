package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrainingQuestionValidationTest {
    @Test
    void rejectsChoiceQuestionWithFewerThanTwoOptions() {
        assertThatThrownBy(() -> TrainingQuestion.restore(
                null, QuestionType.SINGLE_CHOICE, "测试题", Map.of("A", "唯一选项"),
                Set.of("A"), 10, "消防常识", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("两个选项");
    }

    @Test
    void rejectsMultipleChoiceQuestionWithOnlyOneCorrectAnswer() {
        assertThatThrownBy(() -> TrainingQuestion.restore(
                null, QuestionType.MULTIPLE_CHOICE, "测试题", Map.of("A", "选项一", "B", "选项二"),
                Set.of("A"), 10, "消防常识", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("两个正确答案");
    }

    @Test
    void rejectsBlankCategory() {
        assertThatThrownBy(() -> TrainingQuestion.restore(
                null, QuestionType.SINGLE_CHOICE, "测试题", Map.of("A", "选项一", "B", "选项二"),
                Set.of("A"), 10, "", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分类");
    }

    @Test
    void rejectsScoreAboveLimit() {
        assertThatThrownBy(() -> TrainingQuestion.restore(
                null, QuestionType.SINGLE_CHOICE, "测试题", Map.of("A", "选项一", "B", "选项二"),
                Set.of("A"), 1001, "消防常识", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分值");
    }
}
