package com.firesafety.platform.training;

import java.util.List;

public interface TrainingNotificationPort {
    void taskPublished(TrainingTask task, List<TrainingParticipant> participants);
}
