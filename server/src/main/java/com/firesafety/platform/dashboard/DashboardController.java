package com.firesafety.platform.dashboard;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/api/dashboard/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<DashboardSummary> summary(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(service.summary(principal));
    }
}
