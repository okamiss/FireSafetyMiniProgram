package com.firesafety.platform.repair;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcRepairNotificationAdapter implements RepairNotificationPort {
    private final JdbcTemplate jdbc;

    public JdbcRepairNotificationAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void statusChanged(RepairTicket ticket) {
        jdbc.update("""
                INSERT INTO station_message
                    (enterprise_id, recipient_user_id, message_type, title, content, business_type, business_id)
                VALUES (?, ?, 'REPAIR_STATUS', ?, ?, 'REPAIR', ?)
                """, ticket.enterpriseId(), ticket.reporterUserId(), "报修状态已更新",
                "报修工单 #" + ticket.id() + " 当前状态：" + ticket.status().name(), ticket.id());
    }
}
