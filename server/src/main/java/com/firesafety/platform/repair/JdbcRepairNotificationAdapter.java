package com.firesafety.platform.repair;

import com.firesafety.platform.message.CreateStationMessageCommand;
import com.firesafety.platform.message.StationMessageService;
import org.springframework.stereotype.Component;

@Component
public class JdbcRepairNotificationAdapter implements RepairNotificationPort {
    private final StationMessageService messages;

    public JdbcRepairNotificationAdapter(StationMessageService messages) { this.messages = messages; }

    @Override
    public void statusChanged(RepairTicket ticket) {
        messages.create(new CreateStationMessageCommand(
                ticket.enterpriseId(), ticket.reporterUserId(), "REPAIR_STATUS", "报修状态已更新",
                "报修工单 #" + ticket.id() + " 当前状态：" + ticket.status().name(), "REPAIR", ticket.id()));
    }
}
