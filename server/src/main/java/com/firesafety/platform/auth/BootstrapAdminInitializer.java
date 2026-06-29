package com.firesafety.platform.auth;

import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final BootstrapAdminProperties properties;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminInitializer(
            BootstrapAdminProperties properties, UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var username = properties.username();
        var password = properties.password();
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) return;
        if (username == null || username.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME 和至少 12 位密码必须同时配置");
        }
        if (users.findByUsername(username).isPresent()) return;
        var displayName = properties.displayName() == null || properties.displayName().isBlank()
                ? "系统管理员" : properties.displayName();
        users.save(UserAccount.admin(username, passwordEncoder.encode(password), displayName, UserRole.SUPER_ADMIN));
        LOGGER.info("Bootstrap super administrator created: {}", username);
    }
}
