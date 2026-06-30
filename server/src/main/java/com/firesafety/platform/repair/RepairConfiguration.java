package com.firesafety.platform.repair;

import com.firesafety.platform.audit.AuditLogPort;
import com.firesafety.platform.security.DataScopeService;
import com.firesafety.platform.security.EnterpriseScopeResolver;
import com.firesafety.platform.file.FileStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepairConfiguration {
    @Bean
    DataScopeService dataScopeService(EnterpriseScopeResolver resolver) {
        return new DataScopeService(resolver);
    }

    @Bean
    RepairService repairService(
            RepairTicketRepository tickets,
            RepairHistoryRepository history,
            DataScopeService dataScope,
            RepairNotificationPort notifications,
            AuditLogPort audit) {
        return new RepairService(tickets, history, dataScope, notifications, audit);
    }

    @Bean
    RepairAttachmentService repairAttachmentService(
            RepairService repairs, RepairAttachmentRepository attachments, FileStorage storage) {
        return new RepairAttachmentService(repairs, attachments, storage);
    }
}
