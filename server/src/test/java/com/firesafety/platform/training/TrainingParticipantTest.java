package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.common.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TrainingParticipantTest {
    @Test
    void tracksSequentialAttemptsHighestScoreAndPassState() {
        var participant = TrainingParticipant.assigned(10L, 20L, 30L);
        var now = Instant.parse("2026-06-30T00:00:00Z");

        var first = participant.recordAttempt(45, 60, 3, now);
        var second = participant.recordAttempt(80, 60, 3, now.plusSeconds(60));
        var third = participant.recordAttempt(70, 60, 3, now.plusSeconds(120));

        assertThat(first).extracting(TrainingAttemptDecision::attemptNo, TrainingAttemptDecision::passed)
                .containsExactly(1, false);
        assertThat(second).extracting(TrainingAttemptDecision::attemptNo, TrainingAttemptDecision::passed)
                .containsExactly(2, true);
        assertThat(third.attemptNo()).isEqualTo(3);
        assertThat(participant.bestScore()).isEqualTo(80);
        assertThat(participant.passed()).isTrue();
        assertThat(participant.completedAt()).isEqualTo(now.plusSeconds(60));
    }

    @Test
    void rejectsFourthAttempt() {
        var participant = TrainingParticipant.assigned(10L, 20L, 30L);
        var now = Instant.parse("2026-06-30T00:00:00Z");
        participant.recordAttempt(10, 60, 3, now);
        participant.recordAttempt(20, 60, 3, now);
        participant.recordAttempt(30, 60, 3, now);

        assertThatThrownBy(() -> participant.recordAttempt(100, 60, 3, now))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("ATTEMPT_LIMIT"));
    }
}
