package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingCertificateController {
    private final TrainingCertificateService service;

    public TrainingCertificateController(TrainingCertificateService service) { this.service = service; }

    @GetMapping({"/api/miniapp/training/certificates", "/api/admin/training/certificates"})
    public ApiResponse<List<CertificateResponse>> list(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal).stream().map(CertificateResponse::from).toList());
    }

    @GetMapping("/api/training/certificates/{id}/content")
    public ResponseEntity<ByteArrayResource> content(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        var certificate = service.findAuthorized(principal, id);
        var bytes = service.loadContent(principal, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(certificate.certificateNo() + ".pdf", StandardCharsets.UTF_8)
                        .build().toString())
                .body(new ByteArrayResource(bytes));
    }

    public record CertificateResponse(
            Long id, Long recordId, Long taskId, String certificateNo, Instant issuedAt, String contentUrl) {
        static CertificateResponse from(TrainingCertificate value) {
            return new CertificateResponse(value.id(), value.recordId(), value.taskId(), value.certificateNo(),
                    value.issuedAt(), "/api/training/certificates/" + value.id() + "/content");
        }
    }
}
