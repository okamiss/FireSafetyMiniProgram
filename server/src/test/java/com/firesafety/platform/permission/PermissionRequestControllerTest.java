package com.firesafety.platform.permission;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PermissionRequestControllerTest {
    @Mock private PermissionRequestService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PermissionRequestController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enterpriseAdminSubmitsEmployeeRequest() throws Exception {
        var principal = new SessionPrincipal(11L, UserRole.ENTERPRISE_ADMIN, 20L, "企业管理员");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "access"));
        var request = PermissionRequest.employee(20L, 11L, "李四", "13900000000");
        request.assignId(7L);
        when(service.requestEmployee(principal, "李四", "13900000000")).thenReturn(request);

        mockMvc.perform(post("/api/miniapp/permission-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"李四\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void platformOperatorApprovesRequestWithRemark() throws Exception {
        var principal = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "access"));
        var request = PermissionRequest.employee(20L, 11L, "李四", "13900000000");
        request.assignId(7L);
        request.approve(1L, "资料已核验");
        when(service.approve(principal, 7L, "资料已核验")).thenReturn(request);

        mockMvc.perform(post("/api/admin/permission-requests/7/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"资料已核验\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(service).approve(principal, 7L, "资料已核验");
    }
}
