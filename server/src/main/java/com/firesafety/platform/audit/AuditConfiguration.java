package com.firesafety.platform.audit;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfiguration {
    @Bean
    AuditLogService auditLogService(OperationLogRepository logs, Clock clock) {
        return new AuditLogService(logs, clock);
    }
}
