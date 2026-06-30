package com.firesafety.platform.training.persistence;

import com.firesafety.platform.training.TrainingRecord;
import com.firesafety.platform.training.TrainingRecordRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTrainingRecordRepository implements TrainingRecordRepository {
    private final TrainingRecordJpaRepository jpa;

    public JpaTrainingRecordRepository(TrainingRecordJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public TrainingRecord save(TrainingRecord value) {
        var entity = new TrainingRecordEntity();
        entity.taskId = value.taskId();
        entity.userId = value.userId();
        entity.enterpriseId = value.enterpriseId();
        entity.score = value.score();
        entity.passed = value.passed();
        entity.attemptNo = value.attemptNo();
        entity.submittedAt = value.submittedAt();
        return toDomain(jpa.save(entity));
    }

    @Override public List<TrainingRecord> findByTaskIdAndUserId(Long taskId, Long userId) {
        return jpa.findAllByTaskIdAndUserIdOrderByAttemptNoAsc(taskId, userId).stream().map(this::toDomain).toList();
    }
    @Override public List<TrainingRecord> findAll() { return jpa.findAll().stream().map(this::toDomain).toList(); }

    private TrainingRecord toDomain(TrainingRecordEntity entity) {
        return TrainingRecord.restore(entity.id, entity.taskId, entity.userId, entity.enterpriseId,
                entity.score, entity.passed, entity.attemptNo, entity.submittedAt);
    }
}
