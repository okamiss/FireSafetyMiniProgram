package com.firesafety.platform.export;

import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.training.TrainingAnswerDetailRepository;
import com.firesafety.platform.training.TrainingRecordRepository;
import com.firesafety.platform.training.TrainingTaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExportConfiguration {
    @Bean
    DataExportService dataExportService(
            RepairTicketRepository repairs,
            TrainingRecordRepository records,
            TrainingAnswerDetailRepository answerDetails,
            TrainingTaskRepository tasks,
            EnterpriseRepository enterprises,
            UserAccountRepository users) {
        return new DataExportService(repairs, records, answerDetails, tasks, enterprises, users);
    }
}
