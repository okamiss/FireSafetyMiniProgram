package com.firesafety.platform.repair;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.security.DataScopeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RepairService {
    private final RepairTicketRepository tickets;
    private final RepairHistoryRepository history;
    private final DataScopeService dataScope;
    private final RepairNotificationPort notifications;

    public RepairService(
            RepairTicketRepository tickets,
            RepairHistoryRepository history,
            DataScopeService dataScope,
            RepairNotificationPort notifications) {
        this.tickets = tickets;
        this.history = history;
        this.dataScope = dataScope;
        this.notifications = notifications;
    }

    public RepairTicket create(SessionPrincipal reporter, CreateRepairCommand command) {
        if (reporter.enterpriseId() == null
                || (reporter.role() != UserRole.EMPLOYEE && reporter.role() != UserRole.ENTERPRISE_ADMIN)) {
            throw new BusinessException("FORBIDDEN", "只有企业用户可以提交报修", HttpStatus.FORBIDDEN);
        }
        var saved = tickets.save(RepairTicket.create(reporter.enterpriseId(), reporter.userId(), command));
        history.save(RepairHistory.created(saved.id(), reporter.userId()));
        return saved;
    }

    public RepairTicket accept(SessionPrincipal operator, Long repairId, String remark) {
        requirePlatformRole(operator);
        var ticket = findForUpdate(repairId);
        var from = ticket.status();
        ticket.accept(operator.userId());
        return saveTransition(ticket, from, operator.userId(), remark);
    }

    public RepairTicket complete(SessionPrincipal operator, Long repairId, String result) {
        requirePlatformRole(operator);
        var ticket = findForUpdate(repairId);
        var from = ticket.status();
        ticket.complete(operator.userId(), result);
        return saveTransition(ticket, from, operator.userId(), result);
    }

    public RepairTicket close(SessionPrincipal operator, Long repairId, String remark) {
        requirePlatformRole(operator);
        var ticket = findForUpdate(repairId);
        var from = ticket.status();
        ticket.close();
        return saveTransition(ticket, from, operator.userId(), remark);
    }

    @Transactional(readOnly = true)
    public List<RepairTicket> list(SessionPrincipal principal) {
        if (principal.role() == UserRole.SUPER_ADMIN || principal.role() == UserRole.PLATFORM_OPERATOR) {
            return tickets.findAll();
        }
        if (principal.role() == UserRole.EMPLOYEE) {
            return tickets.findByReporterUserId(principal.userId());
        }
        if (principal.role() == UserRole.ENTERPRISE_ADMIN) {
            return tickets.findByEnterpriseIds(dataScope.visibleEnterpriseIds(principal));
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public RepairTicket detail(SessionPrincipal principal, Long repairId) {
        return list(principal).stream().filter(ticket -> ticket.id().equals(repairId)).findFirst()
                .orElseThrow(() -> new BusinessException("REPAIR_NOT_FOUND", "报修工单不存在", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<RepairHistory> history(SessionPrincipal principal, Long repairId) {
        detail(principal, repairId);
        return history.findByRepairId(repairId);
    }

    private RepairTicket saveTransition(
            RepairTicket ticket, RepairStatus from, Long operatorUserId, String remark) {
        var saved = tickets.save(ticket);
        history.save(RepairHistory.transition(
                saved.id(), from, saved.status(), operatorUserId, remark));
        notifications.statusChanged(saved);
        return saved;
    }

    private RepairTicket findForUpdate(Long repairId) {
        return tickets.findByIdForUpdate(repairId)
                .orElseThrow(() -> new BusinessException("REPAIR_NOT_FOUND", "报修工单不存在", HttpStatus.NOT_FOUND));
    }

    private void requirePlatformRole(SessionPrincipal operator) {
        if (operator.role() != UserRole.SUPER_ADMIN && operator.role() != UserRole.PLATFORM_OPERATOR) {
            throw new BusinessException("FORBIDDEN", "没有报修处理权限", HttpStatus.FORBIDDEN);
        }
    }
}
