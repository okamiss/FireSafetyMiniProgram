package com.firesafety.platform.repair;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class RepairAttachmentController {
    private final RepairAttachmentService service;

    public RepairAttachmentController(RepairAttachmentService service) { this.service = service; }

    @PostMapping(value = "/api/miniapp/repairs/{repairId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<AttachmentResponse> upload(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable Long repairId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(AttachmentResponse.from(service.upload(principal, repairId, file)));
    }

    @GetMapping({"/api/miniapp/repairs/{repairId}/attachments", "/api/admin/repairs/{repairId}/attachments"})
    public ApiResponse<List<AttachmentResponse>> list(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long repairId) {
        return ApiResponse.ok(service.list(principal, repairId).stream().map(AttachmentResponse::from).toList());
    }

    @GetMapping("/api/repair-attachments/{attachmentId}/content")
    public ResponseEntity<ByteArrayResource> download(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long attachmentId) {
        var file = service.download(principal, attachmentId);
        var resource = new ByteArrayResource(file.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.metadata().contentType()))
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.metadata().originalName(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    public record AttachmentResponse(
            Long id, Long repairId, String originalName, String contentType, long fileSize,
            Instant createdAt, String contentUrl) {
        static AttachmentResponse from(RepairAttachment value) {
            return new AttachmentResponse(value.id(), value.repairId(), value.originalName(), value.contentType(),
                    value.fileSize(), value.createdAt(), "/api/repair-attachments/" + value.id() + "/content");
        }
    }
}
