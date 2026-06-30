package com.firesafety.platform.repair;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
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
class RepairControllerTest {
    @Mock private RepairService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RepairController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void employeeSubmitsRepair() throws Exception {
        var principal = new SessionPrincipal(11L, UserRole.EMPLOYEE, 20L, "张三");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "access"));
        var command = new CreateRepairCommand(
                RepairUrgency.HIGH, "消防设施", "一号楼", "灭火器压力不足", "张三", "13800000000");
        var ticket = RepairTicket.create(20L, 11L, command);
        ticket.assignId(8L);
        when(service.create(principal, command)).thenReturn(ticket);

        mockMvc.perform(post("/api/miniapp/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"urgency":"HIGH","faultType":"消防设施","location":"一号楼",
                                 "description":"灭火器压力不足","contactName":"张三","contactPhone":"13800000000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.status").value("PENDING_ACCEPTANCE"));
    }

    @Test
    void operatorAcceptsRepair() throws Exception {
        var principal = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "access"));
        var ticket = RepairTicket.create(20L, 11L, new CreateRepairCommand(
                RepairUrgency.HIGH, "消防设施", "一号楼", "故障", "张三", "13800000000"));
        ticket.assignId(8L);
        ticket.accept(1L);
        when(service.accept(principal, 8L, "已安排处理", "127.0.0.1")).thenReturn(ticket);

        mockMvc.perform(post("/api/admin/repairs/8/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"已安排处理\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }
}
