package com.firesafety.platform.message;

public record DeliveryOutcome(
        ExternalDeliveryStatus status, String errorCode, String errorMessage) {
    public static DeliveryOutcome sent() { return new DeliveryOutcome(ExternalDeliveryStatus.SENT, null, null); }
    public static DeliveryOutcome skipped(String code, String message) {
        return new DeliveryOutcome(ExternalDeliveryStatus.SKIPPED, code, message);
    }
    public static DeliveryOutcome failed(String code, String message) {
        return new DeliveryOutcome(ExternalDeliveryStatus.FAILED, code, message);
    }
}
