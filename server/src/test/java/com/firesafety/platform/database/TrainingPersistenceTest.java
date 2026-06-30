package com.firesafety.platform.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.training.QuestionType;
import com.firesafety.platform.training.TrainingAnswerDetail;
import com.firesafety.platform.training.TrainingAnswerDetailRepository;
import com.firesafety.platform.training.TrainingAttemptDecision;
import com.firesafety.platform.training.TrainingParticipant;
import com.firesafety.platform.training.TrainingParticipantRepository;
import com.firesafety.platform.training.TrainingQuestion;
import com.firesafety.platform.training.TrainingQuestionRepository;
import com.firesafety.platform.training.TrainingRecord;
import com.firesafety.platform.training.TrainingRecordRepository;
import com.firesafety.platform.training.TrainingTask;
import com.firesafety.platform.training.TrainingTaskRepository;
import com.firesafety.platform.training.TrainingTaskStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrainingPersistenceTest {
    @Autowired EnterpriseRepository enterprises;
    @Autowired UserAccountRepository users;
    @Autowired TrainingQuestionRepository questions;
    @Autowired TrainingTaskRepository tasks;
    @Autowired TrainingParticipantRepository participants;
    @Autowired TrainingRecordRepository records;
    @Autowired TrainingAnswerDetailRepository details;

    @Test
    void persistsTrainingTaskParticipantAttemptAndAnswerDetails() {
        var enterprise = enterprises.save(Enterprise.headquarters("培训企业", "联系人", "13800000000"));
        var employee = users.save(UserAccount.employee(
                enterprise.id(), "张三", "13800000001", UserRole.EMPLOYEE));
        var operator = users.save(UserAccount.admin(
                "training-operator", "unused", "培训运营", UserRole.PLATFORM_OPERATOR));
        var options = new LinkedHashMap<String, String>();
        options.put("A", "使用灭火器");
        options.put("B", "乘坐电梯");
        var question = questions.save(TrainingQuestion.restore(
                null, QuestionType.SINGLE_CHOICE, "发现初起火灾怎么办？", options,
                Set.of("A"), 100, "消防常识", "优先保证人员安全", true));
        var task = tasks.save(TrainingTask.restore(
                null, "消防基础培训", "完成答题", Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-07-31T00:00:00Z"), 60, 3, TrainingTaskStatus.DRAFT,
                Set.of(question.id()), Set.of(enterprise.id()), Set.of(), operator.id(), Instant.now(), null));
        var participant = participants.save(TrainingParticipant.assigned(
                task.id(), employee.id(), enterprise.id()));
        var submittedAt = Instant.parse("2026-06-30T00:00:00Z");
        var decision = participant.recordAttempt(100, 60, 3, submittedAt);
        participants.save(participant);
        var record = records.save(TrainingRecord.create(
                task.id(), employee.id(), enterprise.id(), decision, submittedAt));
        details.saveAll(List.of(new TrainingAnswerDetail(
                null, record.id(), question.id(), Set.of("A"), true, 100)));

        assertThat(questions.findAllById(Set.of(question.id()))).hasSize(1);
        assertThat(tasks.findByIdForUpdate(task.id())).isPresent();
        assertThat(participants.findByTaskIdAndUserIdForUpdate(task.id(), employee.id()))
                .get().extracting(TrainingParticipant::attemptsUsed, TrainingParticipant::bestScore)
                .containsExactly(1, 100);
        assertThat(records.findByTaskIdAndUserId(task.id(), employee.id())).hasSize(1);
        assertThat(details.findByRecordId(record.id())).hasSize(1);
    }
}
