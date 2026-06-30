package com.firesafety.platform.training.persistence;

import com.firesafety.platform.training.TrainingCertificate;
import com.firesafety.platform.training.TrainingCertificateRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTrainingCertificateRepository implements TrainingCertificateRepository {
    private final TrainingCertificateJpaRepository jpa;

    public JpaTrainingCertificateRepository(TrainingCertificateJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public TrainingCertificate save(TrainingCertificate value) {
        var entity = new TrainingCertificateEntity();
        entity.recordId = value.recordId();
        entity.taskId = value.taskId();
        entity.userId = value.userId();
        entity.enterpriseId = value.enterpriseId();
        entity.certificateNo = value.certificateNo();
        entity.storageKey = value.storageKey();
        entity.issuedAt = value.issuedAt();
        return toDomain(jpa.save(entity));
    }

    @Override public Optional<TrainingCertificate> findByTaskIdAndUserId(Long taskId, Long userId) {
        return jpa.findByTaskIdAndUserId(taskId, userId).map(this::toDomain);
    }
    @Override public Optional<TrainingCertificate> findById(Long id) { return jpa.findById(id).map(this::toDomain); }
    @Override public List<TrainingCertificate> findByUserId(Long userId) {
        return jpa.findAllByUserIdOrderByIssuedAtDesc(userId).stream().map(this::toDomain).toList();
    }
    @Override public List<TrainingCertificate> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private TrainingCertificate toDomain(TrainingCertificateEntity entity) {
        return TrainingCertificate.restore(entity.id, entity.recordId, entity.taskId, entity.userId,
                entity.enterpriseId, entity.certificateNo, entity.storageKey, entity.issuedAt);
    }
}
