package com.firesafety.platform.permission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

interface PermissionRequestJpaRepository extends JpaRepository<PermissionRequestEntity, Long> {
    List<PermissionRequestEntity> findAllByOrderByCreatedAtDesc();
    List<PermissionRequestEntity> findAllByEnterpriseIdOrderByCreatedAtDesc(Long enterpriseId);
    boolean existsByRequestedPhoneAndStatus(String requestedPhone, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from PermissionRequestEntity request where request.id = :id")
    Optional<PermissionRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
