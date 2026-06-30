package com.firesafety.platform.dashboard;

import com.firesafety.platform.repair.RepairStatus;
import java.util.Map;

public record DashboardSummary(
        long repairTotal,
        Map<RepairStatus, Long> repairStatusCounts,
        long trainingAssigned,
        long trainingCompleted,
        long trainingPassed,
        double trainingCompletionRate,
        double trainingPassRate) {
    public DashboardSummary {
        repairStatusCounts = Map.copyOf(repairStatusCounts);
    }
}
