package com.firesafety.platform.repair.persistence;

import com.firesafety.platform.repair.RepairHistory;
import com.firesafety.platform.repair.RepairHistoryRepository;
import com.firesafety.platform.repair.RepairStatus;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRepairHistoryRepository implements RepairHistoryRepository {
    private final RepairHistoryJpaRepository jpa;

    public JpaRepairHistoryRepository(RepairHistoryJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public RepairHistory save(RepairHistory history) {
        var entity = new RepairHistoryEntity();
        entity.repairId = history.repairId();
        entity.fromStatus = history.fromStatus() == null ? null : history.fromStatus().name();
        entity.toStatus = history.toStatus().name();
        entity.operatorUserId = history.operatorUserId();
        entity.remark = history.remark();
        entity.createdAt = history.createdAt();
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<RepairHistory> findByRepairId(Long repairId) {
        return jpa.findAllByRepairIdOrderByCreatedAtAsc(repairId).stream().map(this::toDomain).toList();
    }

    private RepairHistory toDomain(RepairHistoryEntity entity) {
        return new RepairHistory(entity.id, entity.repairId,
                entity.fromStatus == null ? null : RepairStatus.valueOf(entity.fromStatus),
                RepairStatus.valueOf(entity.toStatus), entity.operatorUserId, entity.remark, entity.createdAt);
    }
}
