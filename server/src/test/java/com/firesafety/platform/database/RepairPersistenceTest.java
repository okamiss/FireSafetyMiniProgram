package com.firesafety.platform.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.repair.CreateRepairCommand;
import com.firesafety.platform.repair.RepairHistory;
import com.firesafety.platform.repair.RepairHistoryRepository;
import com.firesafety.platform.repair.RepairStatus;
import com.firesafety.platform.repair.RepairTicket;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.repair.RepairUrgency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepairPersistenceTest {
    @Autowired private EnterpriseRepository enterprises;
    @Autowired private UserAccountRepository users;
    @Autowired private RepairTicketRepository tickets;
    @Autowired private RepairHistoryRepository history;

    @Test
    void persistsTicketTransitionAndHistory() {
        var enterprise = enterprises.save(Enterprise.headquarters("示例企业", "联系人", "13800000000"));
        var employee = users.save(UserAccount.employee(
                enterprise.id(), "张三", "13800000001", UserRole.EMPLOYEE));
        var operator = users.save(UserAccount.admin(
                "operator", "unused", "平台运营", UserRole.PLATFORM_OPERATOR));
        var ticket = tickets.save(RepairTicket.create(enterprise.id(), employee.id(), new CreateRepairCommand(
                RepairUrgency.HIGH, "消防设施", "一号楼", "灭火器压力不足", "张三", "13800000001")));
        history.save(RepairHistory.created(ticket.id(), employee.id()));

        ticket.accept(operator.id());
        tickets.save(ticket);
        history.save(RepairHistory.transition(
                ticket.id(), RepairStatus.PENDING_ACCEPTANCE, RepairStatus.PROCESSING,
                operator.id(), "已受理"));

        assertThat(tickets.findById(ticket.id())).get()
                .extracting(RepairTicket::status, RepairTicket::handlerUserId)
                .containsExactly(RepairStatus.PROCESSING, operator.id());
        assertThat(history.findByRepairId(ticket.id())).extracting(RepairHistory::toStatus)
                .containsExactly(RepairStatus.PENDING_ACCEPTANCE, RepairStatus.PROCESSING);
    }
}
