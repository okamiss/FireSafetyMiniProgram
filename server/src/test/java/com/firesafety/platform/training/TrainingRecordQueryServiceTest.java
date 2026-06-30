package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingRecordQueryServiceTest {
    @Mock private TrainingRecordRepository records;
    @Mock private TrainingTaskRepository tasks;
    @Mock private UserAccountRepository users;
    @Mock private EnterpriseRepository enterprises;

    @Test
    void returnsEnrichedRecordsForPlatformOperator() {
        var submittedAt = Instant.parse("2026-06-30T01:00:00Z");
        when(records.findAll()).thenReturn(List.of(
                TrainingRecord.restore(501L, 10L, 20L, 30L, 80, true, 2, submittedAt)));
        when(tasks.findAll()).thenReturn(List.of(TrainingTask.restore(
                10L, "消防基础培训", "完成答题", submittedAt.minusSeconds(3600), submittedAt.plusSeconds(3600),
                60, 3, TrainingTaskStatus.PUBLISHED, Set.of(1L), Set.of(30L), Set.of(),
                1L, submittedAt.minusSeconds(7200), submittedAt.minusSeconds(3600))));
        var user = UserAccount.employee(30L, "张三", "13800000000", UserRole.EMPLOYEE);
        user.assignId(20L);
        when(users.findAll()).thenReturn(List.of(user));
        when(enterprises.findAll()).thenReturn(List.of(
                Enterprise.restore(30L, null, "示例企业", "联系人", "13800000001", true)));
        var service = new TrainingRecordQueryService(records, tasks, users, enterprises);

        var result = service.list(new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "运营"));

        assertThat(result).singleElement().satisfies(record -> {
            assertThat(record.taskTitle()).isEqualTo("消防基础培训");
            assertThat(record.userName()).isEqualTo("张三");
            assertThat(record.enterpriseName()).isEqualTo("示例企业");
            assertThat(record.score()).isEqualTo(80);
            assertThat(record.passed()).isTrue();
        });
    }
}
