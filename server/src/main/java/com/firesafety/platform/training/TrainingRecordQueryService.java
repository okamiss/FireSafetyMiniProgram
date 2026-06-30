package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class TrainingRecordQueryService {
    private final TrainingRecordRepository records;
    private final TrainingTaskRepository tasks;
    private final UserAccountRepository users;
    private final EnterpriseRepository enterprises;

    public TrainingRecordQueryService(
            TrainingRecordRepository records,
            TrainingTaskRepository tasks,
            UserAccountRepository users,
            EnterpriseRepository enterprises) {
        this.records = records;
        this.tasks = tasks;
        this.users = users;
        this.enterprises = enterprises;
    }

    public java.util.List<TrainingRecordView> list(SessionPrincipal operator) {
        requirePlatformRole(operator);
        Map<Long, TrainingTask> taskById = tasks.findAll().stream()
                .collect(Collectors.toMap(TrainingTask::id, Function.identity()));
        Map<Long, UserAccount> userById = users.findAll().stream()
                .collect(Collectors.toMap(UserAccount::id, Function.identity()));
        Map<Long, Enterprise> enterpriseById = enterprises.findAll().stream()
                .collect(Collectors.toMap(Enterprise::id, Function.identity()));
        return records.findAll().stream()
                .sorted(Comparator.comparing(TrainingRecord::submittedAt).reversed())
                .map(record -> toView(record, taskById, userById, enterpriseById))
                .toList();
    }

    private TrainingRecordView toView(
            TrainingRecord record,
            Map<Long, TrainingTask> taskById,
            Map<Long, UserAccount> userById,
            Map<Long, Enterprise> enterpriseById) {
        var task = taskById.get(record.taskId());
        var user = userById.get(record.userId());
        var enterprise = enterpriseById.get(record.enterpriseId());
        return new TrainingRecordView(
                record.id(), record.taskId(), task == null ? "任务已删除" : task.title(),
                record.userId(), user == null ? "账号已删除" : user.displayName(),
                record.enterpriseId(), enterprise == null ? "企业已删除" : enterprise.name(),
                record.score(), record.passed(), record.attemptNo(), record.submittedAt());
    }

    private void requirePlatformRole(SessionPrincipal operator) {
        if (operator == null || (operator.role() != UserRole.SUPER_ADMIN
                && operator.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有培训记录查看权限", HttpStatus.FORBIDDEN);
        }
    }
}
