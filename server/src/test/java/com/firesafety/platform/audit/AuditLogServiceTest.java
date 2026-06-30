package com.firesafety.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditLogServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-30T02:30:00Z");
    private final MemoryLogs repository = new MemoryLogs();
    private final AuditLogService service =
            new AuditLogService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    private final SessionPrincipal operator =
            new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

    @Test
    void recordsAndListsImmutableOperationLog() {
        service.record(operator, AuditModule.PERMISSION, AuditAction.APPROVE,
                8L, "资料已核验", "127.0.0.1");

        var values = service.list(operator);
        assertThat(values).singleElement().satisfies(value -> {
            assertThat(value.operatorId()).isEqualTo(1L);
            assertThat(value.module()).isEqualTo(AuditModule.PERMISSION);
            assertThat(value.action()).isEqualTo(AuditAction.APPROVE);
            assertThat(value.businessId()).isEqualTo(8L);
            assertThat(value.createdAt()).isEqualTo(NOW);
        });
    }

    @Test
    void rejectsAuditQueryFromEnterpriseUser() {
        var employee = new SessionPrincipal(11L, UserRole.EMPLOYEE, 20L, "员工");

        assertThatThrownBy(() -> service.list(employee))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("FORBIDDEN"));
    }

    private static final class MemoryLogs implements OperationLogRepository {
        private final List<OperationLog> values = new ArrayList<>();
        private long sequence = 1;

        @Override public OperationLog save(OperationLog value) {
            var saved = value.id() == null ? value.withId(sequence++) : value;
            values.add(saved);
            return saved;
        }

        @Override public List<OperationLog> findAll() { return List.copyOf(values); }
    }
}
