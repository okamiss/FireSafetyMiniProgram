package com.firesafety.platform.permission;

public interface PermissionNotificationPort {
    void approved(PermissionRequest request);
    void rejected(PermissionRequest request);
}
