package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingAttemptServiceTest {
    @Mock private TrainingParticipantRepository participants;
    @Mock private TrainingTaskRepository tasks;
    @Mock private TrainingQuestionRepository questions;
    @Mock private TrainingRecordRepository records;
    @Mock private TrainingAnswerDetailRepository answerDetails;
    @Mock private TrainingCertificatePort certificates;
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
    private final SessionPrincipal employee = new SessionPrincipal(20L, UserRole.EMPLOYEE, 30L, "张三");

    @Test
    void submitsCompletePaperAndPersistsScoreAttemptAndAnswerDetails() {
        var participant = TrainingParticipant.assigned(10L, 20L, 30L);
        var task = publishedTask();
        when(participants.findByTaskIdAndUserIdForUpdate(10L, 20L)).thenReturn(Optional.of(participant));
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(questions.findAllById(task.questionIds())).thenReturn(List.of(question(101L, "A", 50), question(102L, "B", 50)));
        when(records.save(any())).thenAnswer(invocation -> {
            var record = (TrainingRecord) invocation.getArgument(0);
            record.assignId(501L);
            return record;
        });
        var service = new TrainingAttemptService(
                participants, tasks, questions, records, answerDetails, new TrainingScorer(), certificates, clock);

        var result = service.submit(employee, 10L, Map.of(101L, Set.of("A"), 102L, Set.of("B")));

        assertThat(result.record()).extracting(TrainingRecord::score, TrainingRecord::passed, TrainingRecord::attemptNo)
                .containsExactly(100, true, 1);
        assertThat(participant).extracting(TrainingParticipant::bestScore, TrainingParticipant::attemptsUsed)
                .containsExactly(100, 1);
        verify(participants).save(participant);
        var captor = ArgumentCaptor.forClass(List.class);
        verify(answerDetails).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        var savedDetails = (List<TrainingAnswerDetail>) captor.getValue();
        assertThat(savedDetails).hasSize(2).allMatch(TrainingAnswerDetail::correct);
        verify(certificates).issueIfPassed(task, result.record(), employee);
    }

    @Test
    void rejectsSubmissionWithMissingQuestionAnswer() {
        var participant = TrainingParticipant.assigned(10L, 20L, 30L);
        var task = publishedTask();
        when(participants.findByTaskIdAndUserIdForUpdate(10L, 20L)).thenReturn(Optional.of(participant));
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        when(questions.findAllById(task.questionIds())).thenReturn(List.of(question(101L, "A", 50), question(102L, "B", 50)));
        var service = new TrainingAttemptService(
                participants, tasks, questions, records, answerDetails, new TrainingScorer(), certificates, clock);

        assertThatThrownBy(() -> service.submit(employee, 10L, Map.of(101L, Set.of("A"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("INCOMPLETE_ANSWERS"));
    }

    private TrainingTask publishedTask() {
        return TrainingTask.restore(
                10L, "年度消防培训", "完成在线答题", Instant.parse("2026-06-29T00:00:00Z"),
                Instant.parse("2026-07-30T00:00:00Z"), 60, 3, TrainingTaskStatus.PUBLISHED,
                Set.of(101L, 102L), Set.of(30L), Set.of(), 1L,
                Instant.parse("2026-06-28T00:00:00Z"), Instant.parse("2026-06-28T01:00:00Z"));
    }

    private TrainingQuestion question(Long id, String answer, int score) {
        var options = new LinkedHashMap<String, String>();
        options.put("A", "选项 A");
        options.put("B", "选项 B");
        return TrainingQuestion.restore(id, QuestionType.SINGLE_CHOICE, "测试题", options,
                Set.of(answer), score, "消防常识", "解析", true);
    }
}
