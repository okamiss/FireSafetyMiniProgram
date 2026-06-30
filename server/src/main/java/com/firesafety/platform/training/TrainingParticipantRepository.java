package com.firesafety.platform.training;

import java.util.List;
import java.util.Optional;

public interface TrainingParticipantRepository {
    List<TrainingParticipant> saveAll(List<TrainingParticipant> participants);
    TrainingParticipant save(TrainingParticipant participant);
    Optional<TrainingParticipant> findByTaskIdAndUserId(Long taskId, Long userId);
    Optional<TrainingParticipant> findByTaskIdAndUserIdForUpdate(Long taskId, Long userId);
    List<TrainingParticipant> findByUserId(Long userId);
    List<TrainingParticipant> findByTaskId(Long taskId);
    List<TrainingParticipant> findAll();
}
