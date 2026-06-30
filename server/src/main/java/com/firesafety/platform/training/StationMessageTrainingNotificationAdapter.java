package com.firesafety.platform.training;

import com.firesafety.platform.message.CreateStationMessageCommand;
import com.firesafety.platform.message.StationMessageService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StationMessageTrainingNotificationAdapter implements TrainingNotificationPort {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"));
    private final StationMessageService messages;

    public StationMessageTrainingNotificationAdapter(StationMessageService messages) { this.messages = messages; }

    @Override
    public void taskPublished(TrainingTask task, List<TrainingParticipant> participants) {
        for (var participant : participants) {
            messages.create(new CreateStationMessageCommand(
                    participant.enterpriseId(), participant.userId(), "TRAINING_TASK", "新的消防培训任务",
                    task.title() + "，请于 " + TIME_FORMAT.format(task.endAt()) + " 前完成。",
                    "TRAINING", task.id()));
        }
    }
}
