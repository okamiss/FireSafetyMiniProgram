package com.firesafety.platform.audit.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperationLogJpaRepository extends JpaRepository<OperationLogEntity, Long> {
    List<OperationLogEntity> findAllByOrderByCreatedAtDesc();
}
