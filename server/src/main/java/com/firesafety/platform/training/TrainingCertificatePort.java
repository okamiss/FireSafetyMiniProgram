package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import java.util.Optional;

public interface TrainingCertificatePort {
    Optional<TrainingCertificate> issueIfPassed(
            TrainingTask task, TrainingRecord record, SessionPrincipal principal);
}
