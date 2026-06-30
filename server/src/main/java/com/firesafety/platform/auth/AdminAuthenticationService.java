package com.firesafety.platform.auth;

import com.firesafety.platform.audit.AuditAction;
import com.firesafety.platform.audit.AuditLogPort;
import com.firesafety.platform.audit.AuditModule;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AdminAuthenticationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessions;
    private final AuditLogPort audit;

    public AdminAuthenticationService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            SessionService sessions,
            AuditLogPort audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.audit = audit;
    }

    public AuthenticationResult login(String username, String password) {
        return login(username, password, null);
    }

    public AuthenticationResult login(String username, String password, String ipAddress) {
        UserAccount user = users.findByUsername(username)
                .filter(UserAccount::enabled)
                .filter(this::isPlatformAccount)
                .filter(account -> passwordEncoder.matches(password, account.passwordHash()))
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED));
        var principal = user.toPrincipal();
        audit.record(principal, AuditModule.AUTH, AuditAction.ADMIN_LOGIN, user.id(), null, ipAddress);
        return new AuthenticationResult(principal, sessions.create(principal));
    }

    private boolean isPlatformAccount(UserAccount user) {
        return user.role() == UserRole.SUPER_ADMIN || user.role() == UserRole.PLATFORM_OPERATOR;
    }
}
