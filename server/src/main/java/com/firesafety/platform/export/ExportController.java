package com.firesafety.platform.export;

import com.firesafety.platform.auth.SessionPrincipal;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final DataExportService service;

    public ExportController(DataExportService service) { this.service = service; }

    @GetMapping("/api/exports/repairs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ResponseEntity<ByteArrayResource> repairs(@AuthenticationPrincipal SessionPrincipal principal) {
        return response(service.exportRepairs(principal), "报修数据.xlsx");
    }

    @GetMapping("/api/exports/training-records")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ResponseEntity<ByteArrayResource> trainingRecords(
            @AuthenticationPrincipal SessionPrincipal principal) {
        return response(service.exportTrainingRecords(principal), "培训记录及答题明细.xlsx");
    }

    private ResponseEntity<ByteArrayResource> response(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(bytes));
    }
}
