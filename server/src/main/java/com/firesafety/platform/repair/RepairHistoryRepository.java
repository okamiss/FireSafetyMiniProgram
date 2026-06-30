package com.firesafety.platform.repair;

import java.util.List;

public interface RepairHistoryRepository {
    RepairHistory save(RepairHistory history);
    List<RepairHistory> findByRepairId(Long repairId);
}
