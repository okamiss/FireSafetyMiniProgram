package com.firesafety.platform.dashboard;

import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.training.TrainingParticipantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardConfiguration {
    @Bean
    DashboardService dashboardService(
            RepairTicketRepository repairs, TrainingParticipantRepository participants) {
        return new DashboardService(repairs, participants);
    }
}
