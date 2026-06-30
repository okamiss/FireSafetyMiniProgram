package com.firesafety.platform.training.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TrainingParticipantJpaRepository extends JpaRepository<TrainingParticipantEntity, Long> {
    Optional<TrainingParticipantEntity> findByTaskIdAndUserId(Long taskId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrainingParticipantEntity p where p.taskId = :taskId and p.userId = :userId")
    Optional<TrainingParticipantEntity> findForUpdate(
            @Param("taskId") Long taskId, @Param("userId") Long userId);
    List<TrainingParticipantEntity> findAllByUserId(Long userId);
    List<TrainingParticipantEntity> findAllByTaskId(Long taskId);
}
