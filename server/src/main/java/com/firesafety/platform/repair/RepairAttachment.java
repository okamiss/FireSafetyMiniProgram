package com.firesafety.platform.repair;

import java.time.Instant;

public record RepairAttachment(
        Long id,
        Long enterpriseId,
        Long repairId,
        Long uploaderUserId,
        String storageKey,
        String originalName,
        String contentType,
        long fileSize,
        Instant createdAt) {

    public RepairAttachment withId(Long id) {
        return new RepairAttachment(id, enterpriseId, repairId, uploaderUserId, storageKey,
                originalName, contentType, fileSize, createdAt);
    }
}
