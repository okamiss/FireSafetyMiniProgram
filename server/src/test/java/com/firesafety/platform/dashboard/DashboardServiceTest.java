package com.firesafety.platform.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.repair.CreateRepairCommand;
import com.firesafety.platform.repair.RepairStatus;
import com.firesafety.platform.repair.RepairTicket;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.repair.RepairUrgency;
import com.firesafety.platform.training.TrainingParticipant;
import com.firesafety.platform.training.TrainingParticipantRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock private RepairTicketRepository repairs;
    @Mock private TrainingParticipantRepository participants;
    private final SessionPrincipal operator =
            new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

    @Test
    void calculatesRepairDistributionAndDistinctTrainingRates() {
        var pending = ticket(1L);
        var processing = ticket(2L);
        processing.accept(1L);
        var completed = ticket(3L);
        completed.accept(1L);
        completed.complete(1L, "已处理");
        when(repairs.findAll()).thenReturn(List.of(pending, processing, completed));

        var notStarted = TrainingParticipant.assigned(8L, 11L, 20L);
        var attempted = TrainingParticipant.assigned(8L, 12L, 20L);
        attempted.recordAttempt(50, 60, 2, Instant.parse("2026-06-30T01:00:00Z"));
        var passed = TrainingParticipant.assigned(8L, 13L, 20L);
        passed.recordAttempt(80, 60, 2, Instant.parse("2026-06-30T01:05:00Z"));
        when(participants.findAll()).thenReturn(List.of(notStarted, attempted, passed));

        var summary = new DashboardService(repairs, participants).summary(operator);

        assertThat(summary.repairTotal()).isEqualTo(3);
        assertThat(summary.repairStatusCounts()).containsEntry(RepairStatus.PENDING_ACCEPTANCE, 1L)
                .containsEntry(RepairStatus.PROCESSING, 1L)
                .containsEntry(RepairStatus.COMPLETED, 1L)
                .containsEntry(RepairStatus.CLOSED, 0L);
        assertThat(summary.trainingAssigned()).isEqualTo(3);
        assertThat(summary.trainingCompleted()).isEqualTo(2);
        assertThat(summary.trainingPassed()).isEqualTo(1);
        assertThat(summary.trainingCompletionRate()).isEqualTo(66.67);
        assertThat(summary.trainingPassRate()).isEqualTo(50.0);
    }

    @Test
    void rejectsEnterpriseUserDashboardAccess() {
        var employee = new SessionPrincipal(11L, UserRole.EMPLOYEE, 20L, "员工");

        assertThatThrownBy(() -> new DashboardService(repairs, participants).summary(employee))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("FORBIDDEN"));
    }

    private RepairTicket ticket(Long id) {
        var ticket = RepairTicket.create(20L, 11L, new CreateRepairCommand(
                RepairUrgency.NORMAL, "消防设施", "一号楼", "故障", "张三", "13800000000"));
        ticket.assignId(id);
        return ticket;
    }
}
