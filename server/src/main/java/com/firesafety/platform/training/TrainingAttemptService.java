package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrainingAttemptService {
    private final TrainingParticipantRepository participants;
    private final TrainingTaskRepository tasks;
    private final TrainingQuestionRepository questions;
    private final TrainingRecordRepository records;
    private final TrainingAnswerDetailRepository answerDetails;
    private final TrainingScorer scorer;
    private final TrainingCertificatePort certificates;
    private final Clock clock;

    public TrainingAttemptService(
            TrainingParticipantRepository participants,
            TrainingTaskRepository tasks,
            TrainingQuestionRepository questions,
            TrainingRecordRepository records,
            TrainingAnswerDetailRepository answerDetails,
            TrainingScorer scorer,
            TrainingCertificatePort certificates,
            Clock clock) {
        this.participants = participants;
        this.tasks = tasks;
        this.questions = questions;
        this.records = records;
        this.answerDetails = answerDetails;
        this.scorer = scorer;
        this.certificates = certificates;
        this.clock = clock;
    }

    public TrainingAttemptResult submit(
            SessionPrincipal principal, Long taskId, Map<Long, Set<String>> submittedAnswers) {
        requireEnterpriseUser(principal);
        var participant = participants.findByTaskIdAndUserIdForUpdate(taskId, principal.userId())
                .orElseThrow(() -> new BusinessException(
                        "TRAINING_TASK_NOT_FOUND", "培训任务不存在", HttpStatus.NOT_FOUND));
        var task = requireAvailableTask(taskId);
        var now = Instant.now(clock);
        var taskQuestions = questions.findAllById(task.questionIds());
        if (taskQuestions.size() != task.questionIds().size()) {
            throw new BusinessException("INVALID_TASK_QUESTIONS", "培训任务题目数据不完整");
        }
        if (!submittedAnswers.keySet().equals(task.questionIds())
                || submittedAnswers.values().stream().anyMatch(Set::isEmpty)) {
            throw new BusinessException("INCOMPLETE_ANSWERS", "请完成全部题目后再提交");
        }

        var score = scorer.score(taskQuestions, submittedAnswers);
        var decision = participant.recordAttempt(score.totalScore(), task.passScore(), task.maxAttempts(), now);
        participants.save(participant);
        var record = records.save(TrainingRecord.create(
                taskId, principal.userId(), participant.enterpriseId(), decision, now));
        var details = score.details().stream()
                .map(detail -> TrainingAnswerDetail.from(record.id(), detail))
                .toList();
        answerDetails.saveAll(details);
        certificates.issueIfPassed(task, record, principal);
        return new TrainingAttemptResult(record, details, taskQuestions);
    }

    @Transactional(readOnly = true)
    public TrainingPaper paper(SessionPrincipal principal, Long taskId) {
        requireEnterpriseUser(principal);
        var participant = participants.findByTaskIdAndUserId(taskId, principal.userId())
                .orElseThrow(() -> new BusinessException(
                        "TRAINING_TASK_NOT_FOUND", "培训任务不存在", HttpStatus.NOT_FOUND));
        var task = requireAvailableTask(taskId);
        if (participant.attemptsUsed() >= task.maxAttempts()) {
            throw new BusinessException("ATTEMPT_LIMIT", "已达到最大作答次数");
        }
        var taskQuestions = questions.findAllById(task.questionIds());
        if (taskQuestions.size() != task.questionIds().size()) {
            throw new BusinessException("INVALID_TASK_QUESTIONS", "培训任务题目数据不完整");
        }
        return new TrainingPaper(task, participant, taskQuestions);
    }

    @Transactional(readOnly = true)
    public List<AssignedTrainingTask> assignments(SessionPrincipal principal) {
        requireEnterpriseUser(principal);
        return participants.findByUserId(principal.userId()).stream()
                .map(participant -> tasks.findById(participant.taskId())
                        .map(task -> new AssignedTrainingTask(task, participant)))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private TrainingTask requireAvailableTask(Long taskId) {
        var task = tasks.findById(taskId)
                .orElseThrow(() -> new BusinessException(
                        "TRAINING_TASK_NOT_FOUND", "培训任务不存在", HttpStatus.NOT_FOUND));
        var now = Instant.now(clock);
        if (task.status() != TrainingTaskStatus.PUBLISHED) {
            throw new BusinessException("TASK_NOT_PUBLISHED", "培训任务尚未发布");
        }
        if (now.isBefore(task.startAt()) || now.isAfter(task.endAt())) {
            throw new BusinessException("TASK_OUTSIDE_TIME_WINDOW", "当前不在培训任务作答时间内");
        }
        return task;
    }

    private void requireEnterpriseUser(SessionPrincipal principal) {
        if (principal == null || (principal.role() != UserRole.EMPLOYEE
                && principal.role() != UserRole.ENTERPRISE_ADMIN)) {
            throw new BusinessException("FORBIDDEN", "只有参训企业用户可以答题", HttpStatus.FORBIDDEN);
        }
    }
}
