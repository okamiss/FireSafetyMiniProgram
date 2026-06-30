package com.firesafety.platform.training.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TrainingTaskJpaRepository extends JpaRepository<TrainingTaskEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TrainingTaskEntity t where t.id = :id")
    Optional<TrainingTaskEntity> findByIdForUpdate(@Param("id") Long id);
}
