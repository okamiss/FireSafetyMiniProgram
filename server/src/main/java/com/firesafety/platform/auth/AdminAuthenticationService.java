package com.firesafety.platform.auth;

import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AdminAuthenticationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessions;

    public AdminAuthenticationService(
            UserAccountRepository users, PasswordEncoder passwordEncoder, SessionService sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    public AuthenticationResult login(String username, String password) {
        UserAccount user = users.findByUsername(username)
                .filter(UserAccount::enabled)
                .filter(this::isPlatformAccount)
                .filter(account -> passwordEncoder.matches(password, account.passwordHash()))
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED));
        var principal = user.toPrincipal();
        return new AuthenticationResult(principal, sessions.create(principal));
    }

    private boolean isPlatformAccount(UserAccount user) {
        return user.role() == UserRole.SUPER_ADMIN || user.role() == UserRole.PLATFORM_OPERATOR;
    }
}
