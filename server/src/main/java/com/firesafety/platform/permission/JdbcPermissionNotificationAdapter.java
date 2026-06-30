package com.firesafety.platform.permission;

import com.firesafety.platform.message.CreateStationMessageCommand;
import com.firesafety.platform.message.StationMessageService;
import org.springframework.stereotype.Component;

@Component
public class JdbcPermissionNotificationAdapter implements PermissionNotificationPort {
    private final StationMessageService messages;

    public JdbcPermissionNotificationAdapter(StationMessageService messages) {
        this.messages = messages;
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
        messages.create(new CreateStationMessageCommand(
                request.enterpriseId(), request.applicantUserId(), "PERMISSION_REQUEST", title, content,
                "PERMISSION_REQUEST", request.id()));
    }
}
