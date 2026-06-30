package com.firesafety.platform.training.persistence;

import com.firesafety.platform.training.TrainingParticipant;
import com.firesafety.platform.training.TrainingParticipantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTrainingParticipantRepository implements TrainingParticipantRepository {
    private final TrainingParticipantJpaRepository jpa;

    public JpaTrainingParticipantRepository(TrainingParticipantJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public List<TrainingParticipant> saveAll(List<TrainingParticipant> values) {
        var result = new ArrayList<TrainingParticipant>();
        for (var value : values) result.add(save(value));
        return result;
    }

    @Override
    public TrainingParticipant save(TrainingParticipant value) {
        var entity = value.id() == null ? new TrainingParticipantEntity() : jpa.findById(value.id()).orElseThrow();
        entity.taskId = value.taskId();
        entity.userId = value.userId();
        entity.enterpriseId = value.enterpriseId();
        entity.attemptsUsed = value.attemptsUsed();
        entity.bestScore = value.bestScore();
        entity.passed = value.passed();
        entity.completedAt = value.completedAt();
        return toDomain(jpa.save(entity));
    }

    @Override public Optional<TrainingParticipant> findByTaskIdAndUserIdForUpdate(Long taskId, Long userId) {
        return jpa.findForUpdate(taskId, userId).map(this::toDomain);
    }
    @Override public Optional<TrainingParticipant> findByTaskIdAndUserId(Long taskId, Long userId) {
        return jpa.findByTaskIdAndUserId(taskId, userId).map(this::toDomain);
    }
    @Override public List<TrainingParticipant> findByUserId(Long userId) {
        return jpa.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }
    @Override public List<TrainingParticipant> findByTaskId(Long taskId) {
        return jpa.findAllByTaskId(taskId).stream().map(this::toDomain).toList();
    }

    private TrainingParticipant toDomain(TrainingParticipantEntity entity) {
        return TrainingParticipant.restore(entity.id, entity.taskId, entity.userId, entity.enterpriseId,
                entity.attemptsUsed, entity.bestScore, entity.passed, entity.completedAt);
    }
}
