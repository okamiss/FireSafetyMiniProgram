package com.firesafety.platform.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.security.DataScopeService;
import com.firesafety.platform.security.EnterpriseScopeResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepairServiceTest {
    private final MemoryTickets tickets = new MemoryTickets();
    private final MemoryHistory history = new MemoryHistory();
    private final RepairService service = new RepairService(
            tickets, history, new DataScopeService(new FixedScopeResolver()), request -> {});
    private final SessionPrincipal employee =
            new SessionPrincipal(11L, UserRole.EMPLOYEE, 20L, "张三");
    private final SessionPrincipal operator =
            new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

    @Test
    void followsPendingProcessingCompletedClosedStateMachine() {
        var created = service.create(employee, new CreateRepairCommand(
                RepairUrgency.HIGH, "消防设施", "一号楼三层", "灭火器压力不足", "张三", "13800000000"));
        assertThat(created.status()).isEqualTo(RepairStatus.PENDING_ACCEPTANCE);

        var processing = service.accept(operator, created.id(), "已安排处理");
        assertThat(processing.status()).isEqualTo(RepairStatus.PROCESSING);
        var completed = service.complete(operator, created.id(), "已更换灭火器");
        assertThat(completed.status()).isEqualTo(RepairStatus.COMPLETED);
        var closed = service.close(operator, created.id(), "现场确认完成");

        assertThat(closed.status()).isEqualTo(RepairStatus.CLOSED);
        assertThat(history.values).extracting(RepairHistory::toStatus)
                .containsExactly(
                        RepairStatus.PENDING_ACCEPTANCE,
                        RepairStatus.PROCESSING,
                        RepairStatus.COMPLETED,
                        RepairStatus.CLOSED);
    }

    @Test
    void cannotCompleteBeforeAcceptance() {
        var created = service.create(employee, new CreateRepairCommand(
                RepairUrgency.NORMAL, "消防设施", "一号楼", "故障", "张三", "13800000000"));

        assertThatThrownBy(() -> service.complete(operator, created.id(), "直接完成"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code())
                        .isEqualTo("INVALID_REPAIR_STATUS"));
    }

    @Test
    void employeeOnlySeesOwnTicketsWhileEnterpriseAdminSeesScopedEnterprises() {
        service.create(employee, new CreateRepairCommand(
                RepairUrgency.NORMAL, "消防设施", "一号楼", "员工工单", "张三", "13800000000"));
        service.create(new SessionPrincipal(12L, UserRole.EMPLOYEE, 20L, "李四"), new CreateRepairCommand(
                RepairUrgency.NORMAL, "电气线路", "二号楼", "同企业工单", "李四", "13900000000"));
        service.create(new SessionPrincipal(13L, UserRole.EMPLOYEE, 21L, "王五"), new CreateRepairCommand(
                RepairUrgency.NORMAL, "疏散通道", "三号楼", "子企业工单", "王五", "13700000000"));

        assertThat(service.list(employee)).extracting(RepairTicket::reporterUserId).containsExactly(11L);
        var enterpriseAdmin = new SessionPrincipal(10L, UserRole.ENTERPRISE_ADMIN, 20L, "企业管理员");
        assertThat(service.list(enterpriseAdmin)).extracting(RepairTicket::enterpriseId)
                .containsExactlyInAnyOrder(20L, 20L, 21L);
        assertThat(service.list(operator)).hasSize(3);
    }

    private static final class FixedScopeResolver implements EnterpriseScopeResolver {
        @Override public Set<Long> allEnterpriseIds() { return Set.of(20L, 21L); }
        @Override public Set<Long> descendantsIncludingSelf(long enterpriseId) {
            return enterpriseId == 20L ? Set.of(20L, 21L) : Set.of(enterpriseId);
        }
    }

    private static final class MemoryTickets implements RepairTicketRepository {
        private final Map<Long, RepairTicket> values = new HashMap<>();
        private long sequence = 1;
        @Override public RepairTicket save(RepairTicket ticket) {
            if (ticket.id() == null) ticket.assignId(sequence++);
            values.put(ticket.id(), ticket); return ticket;
        }
        @Override public Optional<RepairTicket> findById(Long id) { return Optional.ofNullable(values.get(id)); }
        @Override public Optional<RepairTicket> findByIdForUpdate(Long id) { return findById(id); }
        @Override public List<RepairTicket> findAll() { return List.copyOf(values.values()); }
        @Override public List<RepairTicket> findByEnterpriseIds(Set<Long> ids) {
            return values.values().stream().filter(value -> ids.contains(value.enterpriseId())).toList();
        }
        @Override public List<RepairTicket> findByReporterUserId(Long id) {
            return values.values().stream().filter(value -> value.reporterUserId().equals(id)).toList();
        }
    }

    private static final class MemoryHistory implements RepairHistoryRepository {
        private final List<RepairHistory> values = new ArrayList<>();
        @Override public RepairHistory save(RepairHistory item) { values.add(item); return item; }
        @Override public List<RepairHistory> findByRepairId(Long repairId) {
            return values.stream().filter(value -> value.repairId().equals(repairId)).toList();
        }
    }
}
