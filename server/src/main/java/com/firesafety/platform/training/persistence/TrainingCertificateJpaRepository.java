package com.firesafety.platform.training.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingCertificateJpaRepository extends JpaRepository<TrainingCertificateEntity, Long> {
    Optional<TrainingCertificateEntity> findByTaskIdAndUserId(Long taskId, Long userId);
    List<TrainingCertificateEntity> findAllByUserIdOrderByIssuedAtDesc(Long userId);
}
