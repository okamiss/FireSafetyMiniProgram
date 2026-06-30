package com.firesafety.platform.repair.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RepairHistoryJpaRepository extends JpaRepository<RepairHistoryEntity, Long> {
    List<RepairHistoryEntity> findAllByRepairIdOrderByCreatedAtAsc(Long repairId);
}
