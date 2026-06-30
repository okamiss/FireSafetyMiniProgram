package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingRecordController {
    private final TrainingRecordQueryService service;

    public TrainingRecordController(TrainingRecordQueryService service) { this.service = service; }

    @GetMapping("/api/admin/training/records")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<List<TrainingRecordView>> list(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.list(principal));
    }
}
