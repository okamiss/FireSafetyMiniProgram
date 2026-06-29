package com.firesafety.platform.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firesafety.platform.common.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class AuthControllerTest {
    @Mock private AdminAuthenticationService adminAuthentication;
    @Mock private WeChatAuthenticationService weChatAuthentication;
    @Mock private SessionService sessions;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(adminAuthentication, weChatAuthentication, sessions))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminLoginReturnsStableSessionContract() throws Exception {
        var principal = new SessionPrincipal(1L, UserRole.SUPER_ADMIN, null, "系统管理员");
        when(adminAuthentication.login("admin", "StrongPass123"))
                .thenReturn(new AuthenticationResult(principal, new SessionTokens("access", "refresh", 7200)));

        mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"StrongPass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.user.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.tokens.accessToken").value("access"));
    }

    @Test
    void refreshAndLogoutRotateThenRevokeSession() throws Exception {
        when(sessions.refresh("old-refresh")).thenReturn(new SessionTokens("new-access", "new-refresh", 7200));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer new-access"))
                .andExpect(status().isOk());

        verify(sessions).logout("new-access");
    }

    @Test
    void meReturnsAuthenticatedPrincipal() throws Exception {
        var principal = new SessionPrincipal(2L, UserRole.PLATFORM_OPERATOR, null, "平台运营");
        var authentication = new UsernamePasswordAuthenticationToken(principal, "access");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.role").value("PLATFORM_OPERATOR"));
    }
}
