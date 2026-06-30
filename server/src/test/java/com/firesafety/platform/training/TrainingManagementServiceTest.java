package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingManagementServiceTest {
    @Mock private TrainingQuestionRepository questions;
    @Mock private TrainingTaskRepository tasks;
    @Mock private TrainingParticipantRepository participants;
    @Mock private UserAccountRepository users;
    @Mock private TrainingNotificationPort notifications;
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
    private final SessionPrincipal operator = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "运营");

    @Test
    void publishesTaskAndSnapshotsEnabledParticipantsFromEnterpriseAndExplicitUsers() {
        var task = TrainingTask.restore(
                9L, "年度消防培训", "完成在线答题", Instant.parse("2026-06-29T00:00:00Z"),
                Instant.parse("2026-07-30T00:00:00Z"), 60, 3, TrainingTaskStatus.DRAFT,
                Set.of(101L, 102L), Set.of(10L), Set.of(22L), 1L,
                Instant.parse("2026-06-28T00:00:00Z"), null);
        when(tasks.findByIdForUpdate(9L)).thenReturn(Optional.of(task));
        when(questions.findAllById(Set.of(101L, 102L))).thenReturn(List.of(question(101L, 50), question(102L, 50)));
        var enterpriseEmployee = user(11L, 10L, UserRole.EMPLOYEE, true);
        var explicitEmployee = user(22L, 20L, UserRole.EMPLOYEE, true);
        var disabledEmployee = user(33L, 10L, UserRole.EMPLOYEE, false);
        when(users.findAll()).thenReturn(List.of(enterpriseEmployee, explicitEmployee, disabledEmployee));
        when(tasks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new TrainingManagementService(questions, tasks, participants, users, notifications, clock);

        var published = service.publish(operator, 9L);

        assertThat(published.status()).isEqualTo(TrainingTaskStatus.PUBLISHED);
        var captor = ArgumentCaptor.forClass(List.class);
        verify(participants).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        var saved = (List<TrainingParticipant>) captor.getValue();
        assertThat(saved).extracting(TrainingParticipant::userId).containsExactlyInAnyOrder(11L, 22L);
    }

    @Test
    void rejectsPublishWhenQuestionPointsDoNotReachPassScore() {
        var task = TrainingTask.restore(
                9L, "年度消防培训", "完成在线答题", Instant.parse("2026-06-29T00:00:00Z"),
                Instant.parse("2026-07-30T00:00:00Z"), 60, 3, TrainingTaskStatus.DRAFT,
                Set.of(101L), Set.of(10L), Set.of(), 1L,
                Instant.parse("2026-06-28T00:00:00Z"), null);
        when(tasks.findByIdForUpdate(9L)).thenReturn(Optional.of(task));
        when(questions.findAllById(Set.of(101L))).thenReturn(List.of(question(101L, 50)));
        var service = new TrainingManagementService(questions, tasks, participants, users, notifications, clock);

        assertThatThrownBy(() -> service.publish(operator, 9L))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("INSUFFICIENT_TASK_SCORE"));
    }

    @Test
    void importsAllValidatedQuestionsForPlatformOperator() {
        var first = new CreateTrainingQuestionCommand(
                QuestionType.SINGLE_CHOICE, "题目一", java.util.Map.of("A", "正确", "B", "错误"),
                Set.of("A"), 50, "消防常识", null);
        var second = new CreateTrainingQuestionCommand(
                QuestionType.TRUE_FALSE, "题目二", java.util.Map.of("TRUE", "正确", "FALSE", "错误"),
                Set.of("TRUE"), 50, "消防常识", null);
        when(questions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new TrainingManagementService(questions, tasks, participants, users, notifications, clock);

        var imported = service.importQuestions(operator, List.of(first, second));

        assertThat(imported).hasSize(2);
        verify(questions, times(2)).save(any());
    }

    private TrainingQuestion question(Long id, int score) {
        var options = new LinkedHashMap<String, String>();
        options.put("A", "选项 A");
        options.put("B", "选项 B");
        return TrainingQuestion.restore(id, QuestionType.SINGLE_CHOICE, "测试题", options,
                Set.of("A"), score, "消防常识", null, true);
    }

    private UserAccount user(Long id, Long enterpriseId, UserRole role, boolean enabled) {
        var user = UserAccount.employee(enterpriseId, "用户" + id, "1380000%04d".formatted(id), role);
        user.assignId(id);
        if (!enabled) user.disable();
        return user;
    }
}
