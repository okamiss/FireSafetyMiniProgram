package com.firesafety.platform.training;

import java.util.List;

public interface TrainingAnswerDetailRepository {
    List<TrainingAnswerDetail> saveAll(List<TrainingAnswerDetail> details);
    List<TrainingAnswerDetail> findByRecordId(Long recordId);
}
