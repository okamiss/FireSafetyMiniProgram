package com.firesafety.platform.training;

import java.util.List;
import java.util.Optional;

public interface TrainingTaskRepository {
    TrainingTask save(TrainingTask task);
    Optional<TrainingTask> findById(Long id);
    Optional<TrainingTask> findByIdForUpdate(Long id);
    List<TrainingTask> findAll();
}
