package com.firesafety.platform.repair.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RepairTicketJpaRepository extends JpaRepository<RepairTicketEntity, Long> {
    List<RepairTicketEntity> findAllByOrderByCreatedAtDesc();
    List<RepairTicketEntity> findAllByEnterpriseIdInOrderByCreatedAtDesc(Set<Long> enterpriseIds);
    List<RepairTicketEntity> findAllByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from RepairTicketEntity ticket where ticket.id = :id")
    Optional<RepairTicketEntity> findByIdForUpdate(@Param("id") Long id);
}
