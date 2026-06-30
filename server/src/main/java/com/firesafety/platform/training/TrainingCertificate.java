package com.firesafety.platform.training;

import java.time.Instant;

public record TrainingCertificate(
        Long id,
        Long recordId,
        Long taskId,
        Long userId,
        Long enterpriseId,
        String certificateNo,
        String storageKey,
        Instant issuedAt) {

    public static TrainingCertificate restore(
            Long id, Long recordId, Long taskId, Long userId, Long enterpriseId,
            String certificateNo, String storageKey, Instant issuedAt) {
        return new TrainingCertificate(
                id, recordId, taskId, userId, enterpriseId, certificateNo, storageKey, issuedAt);
    }
}
