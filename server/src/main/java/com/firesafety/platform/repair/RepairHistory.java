package com.firesafety.platform.repair;

import java.time.Instant;

public record RepairHistory(
        Long id,
        Long repairId,
        RepairStatus fromStatus,
        RepairStatus toStatus,
        Long operatorUserId,
        String remark,
        Instant createdAt) {

    public static RepairHistory created(Long repairId, Long operatorUserId) {
        return new RepairHistory(null, repairId, null, RepairStatus.PENDING_ACCEPTANCE,
                operatorUserId, "提交报修", Instant.now());
    }

    public static RepairHistory transition(
            Long repairId,
            RepairStatus fromStatus,
            RepairStatus toStatus,
            Long operatorUserId,
            String remark) {
        return new RepairHistory(null, repairId, fromStatus, toStatus, operatorUserId, remark, Instant.now());
    }
}
