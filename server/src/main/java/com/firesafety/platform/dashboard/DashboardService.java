package com.firesafety.platform.dashboard;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.repair.RepairStatus;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.training.TrainingParticipantRepository;
import java.util.EnumMap;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DashboardService {
    private final RepairTicketRepository repairs;
    private final TrainingParticipantRepository participants;

    public DashboardService(RepairTicketRepository repairs, TrainingParticipantRepository participants) {
        this.repairs = repairs;
        this.participants = participants;
    }

    public DashboardSummary summary(SessionPrincipal principal) {
        requirePlatformRole(principal);
        var repairValues = repairs.findAll();
        var statusCounts = new EnumMap<RepairStatus, Long>(RepairStatus.class);
        for (var status : RepairStatus.values()) statusCounts.put(status, 0L);
        for (var repair : repairValues) statusCounts.compute(repair.status(), (key, value) -> value + 1);

        var participantValues = participants.findAll();
        long completed = participantValues.stream().filter(value -> value.attemptsUsed() > 0).count();
        long passed = participantValues.stream().filter(value -> value.passed()).count();
        return new DashboardSummary(repairValues.size(), statusCounts, participantValues.size(), completed, passed,
                percentage(completed, participantValues.size()), percentage(passed, completed));
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) return 0;
        return Math.round(numerator * 10_000.0 / denominator) / 100.0;
    }

    private void requirePlatformRole(SessionPrincipal principal) {
        if (principal == null || (principal.role() != UserRole.SUPER_ADMIN
                && principal.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有数据看板权限", HttpStatus.FORBIDDEN);
        }
    }
}
