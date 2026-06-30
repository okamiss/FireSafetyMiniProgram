package com.firesafety.platform.training.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingRecordJpaRepository extends JpaRepository<TrainingRecordEntity, Long> {
    List<TrainingRecordEntity> findAllByTaskIdAndUserIdOrderByAttemptNoAsc(Long taskId, Long userId);
}
