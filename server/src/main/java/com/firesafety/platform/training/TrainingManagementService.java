package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrainingManagementService {
    private final TrainingQuestionRepository questions;
    private final TrainingTaskRepository tasks;
    private final TrainingParticipantRepository participants;
    private final UserAccountRepository users;
    private final TrainingNotificationPort notifications;
    private final Clock clock;

    public TrainingManagementService(
            TrainingQuestionRepository questions,
            TrainingTaskRepository tasks,
            TrainingParticipantRepository participants,
            UserAccountRepository users,
            TrainingNotificationPort notifications,
            Clock clock) {
        this.questions = questions;
        this.tasks = tasks;
        this.participants = participants;
        this.users = users;
        this.notifications = notifications;
        this.clock = clock;
    }

    public TrainingTask publish(SessionPrincipal operator, Long taskId) {
        requirePlatformRole(operator);
        var task = tasks.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException(
                        "TRAINING_TASK_NOT_FOUND", "培训任务不存在", HttpStatus.NOT_FOUND));
        var selectedQuestions = questions.findAllById(task.questionIds());
        if (selectedQuestions.size() != task.questionIds().size()
                || selectedQuestions.stream().anyMatch(question -> !question.enabled())) {
            throw new BusinessException("INVALID_TASK_QUESTIONS", "培训任务包含不存在或已停用的题目");
        }
        var totalScore = selectedQuestions.stream().mapToInt(TrainingQuestion::score).sum();
        task.publish(Instant.now(clock), totalScore);

        List<TrainingParticipant> assignments = users.findAll().stream()
                .filter(user -> user.enabled() && user.enterpriseId() != null)
                .filter(user -> user.role() == UserRole.EMPLOYEE || user.role() == UserRole.ENTERPRISE_ADMIN)
                .filter(user -> task.targetEnterpriseIds().contains(user.enterpriseId())
                        || task.targetUserIds().contains(user.id()))
                .map(user -> TrainingParticipant.assigned(task.id(), user.id(), user.enterpriseId()))
                .toList();
        if (assignments.isEmpty()) {
            throw new BusinessException("NO_TRAINING_PARTICIPANTS", "没有找到可参训的启用账号");
        }
        participants.saveAll(assignments);
        notifications.taskPublished(task, assignments);
        return tasks.save(task);
    }

    public TrainingQuestion createQuestion(
            SessionPrincipal operator, CreateTrainingQuestionCommand command) {
        requirePlatformRole(operator);
        return questions.save(TrainingQuestion.restore(null, command.type(), command.title(), command.options(),
                command.correctAnswers(), command.score(), command.category(), command.explanation(), true));
    }

    public List<TrainingQuestion> importQuestions(
            SessionPrincipal operator, List<CreateTrainingQuestionCommand> commands) {
        requirePlatformRole(operator);
        return commands.stream().map(command -> questions.save(TrainingQuestion.restore(
                null, command.type(), command.title(), command.options(), command.correctAnswers(),
                command.score(), command.category(), command.explanation(), true))).toList();
    }

    public TrainingTask createTask(SessionPrincipal operator, CreateTrainingTaskCommand command) {
        requirePlatformRole(operator);
        return tasks.save(TrainingTask.draft(command, operator.userId(), Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<TrainingQuestion> listQuestions(SessionPrincipal operator) {
        requirePlatformRole(operator);
        return questions.findAll();
    }

    @Transactional(readOnly = true)
    public List<TrainingTask> listTasks(SessionPrincipal operator) {
        requirePlatformRole(operator);
        return tasks.findAll();
    }

    private void requirePlatformRole(SessionPrincipal operator) {
        if (operator == null || (operator.role() != UserRole.SUPER_ADMIN
                && operator.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有培训管理权限", HttpStatus.FORBIDDEN);
        }
    }
}
