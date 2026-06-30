package com.firesafety.platform.repair;

@FunctionalInterface
public interface RepairNotificationPort {
    void statusChanged(RepairTicket ticket);
}
