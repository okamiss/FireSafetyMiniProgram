package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.file.FileStorage;
import com.firesafety.platform.organization.EnterpriseRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrainingCertificateService implements TrainingCertificatePort {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final TrainingCertificateRepository certificates;
    private final EnterpriseRepository enterprises;
    private final FileStorage storage;
    private final CertificateRenderer renderer;
    private final String numberPrefix;
    private final String issuerName;

    public TrainingCertificateService(
            TrainingCertificateRepository certificates,
            EnterpriseRepository enterprises,
            FileStorage storage,
            CertificateRenderer renderer,
            String numberPrefix,
            String issuerName) {
        this.certificates = certificates;
        this.enterprises = enterprises;
        this.storage = storage;
        this.renderer = renderer;
        this.numberPrefix = numberPrefix;
        this.issuerName = issuerName;
    }

    @Override
    public Optional<TrainingCertificate> issueIfPassed(
            TrainingTask task, TrainingRecord record, SessionPrincipal principal) {
        if (!record.passed()) return Optional.empty();
        var existing = certificates.findByTaskIdAndUserId(task.id(), principal.userId());
        if (existing.isPresent()) return existing;
        var enterprise = enterprises.findById(record.enterpriseId())
                .orElseThrow(() -> new BusinessException("ENTERPRISE_NOT_FOUND", "所属企业不存在"));
        var passedDate = record.submittedAt().atZone(BUSINESS_ZONE).toLocalDate();
        var certificateNo = "%s-%04d-%06d".formatted(
                numberPrefix, passedDate.getYear(), record.id());
        var content = new CertificateContent(certificateNo, principal.displayName(), enterprise.name(),
                task.title(), passedDate, issuerName);
        var bytes = renderer.render(content);
        var stored = storage.store(bytes, certificateNo + ".pdf", "application/pdf");
        var issuedAt = Instant.now();
        return Optional.of(certificates.save(TrainingCertificate.restore(
                null, record.id(), task.id(), principal.userId(), record.enterpriseId(),
                certificateNo, stored.storageKey(), issuedAt)));
    }

    @Transactional(readOnly = true)
    public List<TrainingCertificate> list(SessionPrincipal principal) {
        if (principal.role() == UserRole.SUPER_ADMIN || principal.role() == UserRole.PLATFORM_OPERATOR) {
            return certificates.findAll();
        }
        return certificates.findByUserId(principal.userId());
    }

    @Transactional(readOnly = true)
    public TrainingCertificate findAuthorized(SessionPrincipal principal, Long certificateId) {
        var certificate = certificates.findById(certificateId)
                .orElseThrow(() -> new BusinessException(
                        "CERTIFICATE_NOT_FOUND", "培训证书不存在", HttpStatus.NOT_FOUND));
        if (principal.role() != UserRole.SUPER_ADMIN && principal.role() != UserRole.PLATFORM_OPERATOR
                && !certificate.userId().equals(principal.userId())) {
            throw new BusinessException("FORBIDDEN", "没有权限查看该证书", HttpStatus.FORBIDDEN);
        }
        return certificate;
    }

    public byte[] loadContent(SessionPrincipal principal, Long certificateId) {
        return storage.load(findAuthorized(principal, certificateId).storageKey());
    }
}
