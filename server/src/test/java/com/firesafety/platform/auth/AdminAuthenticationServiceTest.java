package com.firesafety.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.support.InMemorySessionStore;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminAuthenticationServiceTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final MemoryUsers users = new MemoryUsers();
    private final SessionService sessions = new SessionService(
            new InMemorySessionStore(), Clock.systemUTC(), Duration.ofHours(2), Duration.ofDays(30));
    private final AdminAuthenticationService service = new AdminAuthenticationService(users, encoder, sessions);

    @Test
    void authenticatesEnabledPlatformAccountAndIssuesSession() {
        users.save(UserAccount.admin("admin", encoder.encode("StrongPass123"), "系统管理员", UserRole.SUPER_ADMIN));

        var result = service.login("admin", "StrongPass123");

        assertThat(result.user().role()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(sessions.resolve(result.tokens().accessToken())).contains(result.user());
    }

    @Test
    void rejectsWrongPasswordWithoutLeakingWhichCredentialFailed() {
        users.save(UserAccount.admin("admin", encoder.encode("StrongPass123"), "系统管理员", UserRole.SUPER_ADMIN));

        assertThatThrownBy(() -> service.login("admin", "wrong"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("INVALID_CREDENTIALS"));
    }

    private static final class MemoryUsers implements UserAccountRepository {
        private final Map<Long, UserAccount> byId = new HashMap<>();
        private long sequence = 1;

        @Override
        public UserAccount save(UserAccount user) {
            if (user.id() == null) {
                user.assignId(sequence++);
            }
            byId.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return byId.values().stream().filter(user -> username.equals(user.username())).findFirst();
        }

        @Override
        public Optional<UserAccount> findByPhone(String phone) {
            return byId.values().stream().filter(user -> phone.equals(user.phone())).findFirst();
        }

        @Override
        public Optional<UserAccount> findByOpenid(String openid) {
            return byId.values().stream().filter(user -> openid.equals(user.openid())).findFirst();
        }

        @Override
        public Optional<UserAccount> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }
    }
}
