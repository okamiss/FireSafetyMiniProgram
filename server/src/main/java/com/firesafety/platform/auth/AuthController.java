package com.firesafety.platform.auth;

import com.firesafety.platform.common.ApiResponse;
import com.firesafety.platform.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminAuthenticationService adminAuthentication;
    private final WeChatAuthenticationService weChatAuthentication;
    private final SessionService sessions;

    public AuthController(
            AdminAuthenticationService adminAuthentication,
            WeChatAuthenticationService weChatAuthentication,
            SessionService sessions) {
        this.adminAuthentication = adminAuthentication;
        this.weChatAuthentication = weChatAuthentication;
        this.sessions = sessions;
    }

    @PostMapping("/admin-login")
    public ApiResponse<AuthenticationResult> adminLogin(
            @Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminAuthentication.login(
                request.username(), request.password(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<WeChatLoginResult> weChatLogin(@Valid @RequestBody WeChatLoginRequest request) {
        return ApiResponse.ok(weChatAuthentication.login(request.code()));
    }

    @PostMapping("/wechat-bind-phone")
    public ApiResponse<AuthenticationResult> bindPhone(@Valid @RequestBody WeChatPhoneBindingRequest request) {
        return ApiResponse.ok(weChatAuthentication.bindPhone(request.bindingTicket(), request.phoneCode()));
    }

    @PostMapping("/refresh")
    public ApiResponse<SessionTokens> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(sessions.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED);
        }
        sessions.logout(authorization.substring(BEARER_PREFIX.length()).trim());
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<SessionPrincipal> me(@AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) {
            throw new BusinessException("UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED);
        }
        return ApiResponse.ok(principal);
    }

    public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record WeChatLoginRequest(@NotBlank String code) {}
    public record WeChatPhoneBindingRequest(@NotBlank String bindingTicket, @NotBlank String phoneCode) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
}
