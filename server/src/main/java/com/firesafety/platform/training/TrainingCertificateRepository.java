package com.firesafety.platform.training;

import java.util.List;
import java.util.Optional;

public interface TrainingCertificateRepository {
    TrainingCertificate save(TrainingCertificate certificate);
    Optional<TrainingCertificate> findByTaskIdAndUserId(Long taskId, Long userId);
    Optional<TrainingCertificate> findById(Long id);
    List<TrainingCertificate> findByUserId(Long userId);
    List<TrainingCertificate> findAll();
}
