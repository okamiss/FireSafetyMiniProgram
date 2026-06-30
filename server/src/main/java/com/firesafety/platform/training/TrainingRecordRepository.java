package com.firesafety.platform.training;

import java.util.List;

public interface TrainingRecordRepository {
    TrainingRecord save(TrainingRecord record);
    List<TrainingRecord> findByTaskIdAndUserId(Long taskId, Long userId);
    List<TrainingRecord> findAll();
}
