package com.firesafety.platform.repair.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface FileResourceJpaRepository extends JpaRepository<FileResourceEntity, Long> {
    List<FileResourceEntity> findAllByBusinessTypeAndBusinessIdOrderByCreatedAtAsc(
            String businessType, Long businessId);
    Optional<FileResourceEntity> findByIdAndBusinessType(Long id, String businessType);
}
