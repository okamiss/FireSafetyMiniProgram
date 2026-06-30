package com.firesafety.platform.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.file.FileStorage;
import com.firesafety.platform.file.StoredFile;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingCertificateServiceTest {
    @Mock private TrainingCertificateRepository certificates;
    @Mock private EnterpriseRepository enterprises;
    @Mock private FileStorage storage;
    @Mock private CertificateRenderer renderer;
    private final SessionPrincipal employee = new SessionPrincipal(20L, UserRole.EMPLOYEE, 30L, "张三");

    @Test
    void issuesCertificateOnceForFirstPassingRecord() {
        var record = TrainingRecord.restore(501L, 10L, 20L, 30L, 80, true, 2,
                Instant.parse("2026-06-30T00:00:00Z"));
        when(certificates.findByTaskIdAndUserId(10L, 20L)).thenReturn(Optional.empty());
        when(enterprises.findById(30L)).thenReturn(Optional.of(
                Enterprise.restore(30L, null, "示例消防企业", "联系人", "13800000000", true)));
        when(renderer.render(any())).thenReturn("pdf".getBytes());
        when(storage.store(any(), any(), any())).thenReturn(
                new StoredFile("certificates/cert.pdf", "FS-2026-000501.pdf", "application/pdf", 3));
        when(certificates.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new TrainingCertificateService(
                certificates, enterprises, storage, renderer, "FS", "企业消防安全培训平台");

        var issued = service.issueIfPassed(task(), record, employee);

        assertThat(issued).isPresent().get()
                .extracting(TrainingCertificate::certificateNo, TrainingCertificate::storageKey)
                .containsExactly("FS-2026-000501", "certificates/cert.pdf");
    }

    @Test
    void reusesExistingCertificateInsteadOfGeneratingDuplicate() {
        var record = TrainingRecord.restore(501L, 10L, 20L, 30L, 80, true, 2,
                Instant.parse("2026-06-30T00:00:00Z"));
        var existing = TrainingCertificate.restore(
                1L, 501L, 10L, 20L, 30L, "FS-2026-000501", "certificates/cert.pdf",
                Instant.parse("2026-06-30T00:00:01Z"));
        when(certificates.findByTaskIdAndUserId(10L, 20L)).thenReturn(Optional.of(existing));
        var service = new TrainingCertificateService(
                certificates, enterprises, storage, renderer, "FS", "企业消防安全培训平台");

        assertThat(service.issueIfPassed(task(), record, employee)).contains(existing);
        verify(renderer, never()).render(any());
        verify(storage, never()).store(any(), any(), any());
    }

    private TrainingTask task() {
        return TrainingTask.restore(10L, "年度消防培训", "完成答题",
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-07-31T00:00:00Z"),
                60, 3, TrainingTaskStatus.PUBLISHED, Set.of(101L), Set.of(30L), Set.of(),
                1L, Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-02T00:00:00Z"));
    }
}
