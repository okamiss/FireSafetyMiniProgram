package com.firesafety.platform.message;

public record CreateStationMessageCommand(
        Long enterpriseId,
        Long recipientUserId,
        String messageType,
        String title,
        String content,
        String businessType,
        Long businessId) {}
