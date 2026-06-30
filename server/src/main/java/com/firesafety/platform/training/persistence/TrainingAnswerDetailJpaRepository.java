package com.firesafety.platform.training.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingAnswerDetailJpaRepository extends JpaRepository<TrainingAnswerDetailEntity, Long> {
    List<TrainingAnswerDetailEntity> findAllByRecordIdOrderByIdAsc(Long recordId);
}
