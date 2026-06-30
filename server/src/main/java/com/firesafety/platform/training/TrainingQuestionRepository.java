package com.firesafety.platform.training;

import java.util.List;
import java.util.Set;

public interface TrainingQuestionRepository {
    TrainingQuestion save(TrainingQuestion question);
    List<TrainingQuestion> findAll();
    List<TrainingQuestion> findAllById(Set<Long> ids);
}
