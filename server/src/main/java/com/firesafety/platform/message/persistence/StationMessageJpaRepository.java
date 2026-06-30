package com.firesafety.platform.message.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StationMessageJpaRepository extends JpaRepository<StationMessageEntity, Long> {
    List<StationMessageEntity> findAllByRecipientUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from StationMessageEntity message where message.id = :id")
    Optional<StationMessageEntity> findByIdForUpdate(@Param("id") Long id);
}
