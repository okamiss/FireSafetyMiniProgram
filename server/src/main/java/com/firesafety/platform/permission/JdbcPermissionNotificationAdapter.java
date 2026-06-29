package com.firesafety.platform.permission;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcPermissionNotificationAdapter implements PermissionNotificationPort {
    private final JdbcTemplate jdbc;

    public JdbcPermissionNotificationAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void approved(PermissionRequest request) {
        save(request, "权限申请已通过", "员工账号已开通，可使用微信手机号登录。");
    }

    @Override
    public void rejected(PermissionRequest request) {
        save(request, "权限申请未通过", "驳回原因：" + request.reviewRemark());
    }

    private void save(PermissionRequest request, String title, String content) {
        jdbc.update("""
                INSERT INTO station_message
                    (enterprise_id, recipient_user_id, message_type, title, content, business_type, business_id)
                VALUES (?, ?, 'PERMISSION_REQUEST', ?, ?, 'PERMISSION_REQUEST', ?)
                """, request.enterpriseId(), request.applicantUserId(), title, content, request.id());
    }
}
