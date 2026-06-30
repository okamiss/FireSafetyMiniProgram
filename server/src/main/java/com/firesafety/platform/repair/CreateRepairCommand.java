package com.firesafety.platform.repair;

public record CreateRepairCommand(
        RepairUrgency urgency,
        String faultType,
        String location,
        String description,
        String contactName,
        String contactPhone) {
}
