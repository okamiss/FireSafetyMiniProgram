package com.firesafety.platform.training;

import java.time.LocalDate;

public record CertificateContent(
        String certificateNo,
        String participantName,
        String enterpriseName,
        String taskTitle,
        LocalDate passedDate,
        String issuerName) {}
