package com.firesafety.platform.training.persistence;

import com.firesafety.platform.training.TrainingTask;
import com.firesafety.platform.training.TrainingTaskRepository;
import com.firesafety.platform.training.TrainingTaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTrainingTaskRepository implements TrainingTaskRepository {
    private final TrainingTaskJpaRepository jpa;

    public JpaTrainingTaskRepository(TrainingTaskJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public TrainingTask save(TrainingTask value) {
        var entity = value.id() == null ? new TrainingTaskEntity() : jpa.findById(value.id()).orElseThrow();
        entity.title = value.title();
        entity.description = value.description();
        entity.startAt = value.startAt();
        entity.endAt = value.endAt();
        entity.passScore = value.passScore();
        entity.maxAttempts = value.maxAttempts();
        entity.status = value.status().name();
        entity.createdBy = value.createdBy();
        entity.createdAt = value.createdAt();
        entity.publishedAt = value.publishedAt();
        entity.questionIds.clear(); entity.questionIds.addAll(value.questionIds());
        entity.targetEnterpriseIds.clear(); entity.targetEnterpriseIds.addAll(value.targetEnterpriseIds());
        entity.targetUserIds.clear(); entity.targetUserIds.addAll(value.targetUserIds());
        return toDomain(jpa.save(entity));
    }

    @Override public Optional<TrainingTask> findById(Long id) { return jpa.findById(id).map(this::toDomain); }
    @Override public Optional<TrainingTask> findByIdForUpdate(Long id) { return jpa.findByIdForUpdate(id).map(this::toDomain); }
    @Override public List<TrainingTask> findAll() { return jpa.findAll().stream().map(this::toDomain).toList(); }

    private TrainingTask toDomain(TrainingTaskEntity entity) {
        return TrainingTask.restore(entity.id, entity.title, entity.description, entity.startAt, entity.endAt,
                entity.passScore, entity.maxAttempts, TrainingTaskStatus.valueOf(entity.status), entity.questionIds,
                entity.targetEnterpriseIds, entity.targetUserIds, entity.createdBy, entity.createdAt, entity.publishedAt);
    }
}
