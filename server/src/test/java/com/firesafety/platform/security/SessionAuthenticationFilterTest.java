package com.firesafety.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.SessionProperties;
import com.firesafety.platform.auth.SessionService;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.support.InMemorySessionStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class SessionAuthenticationFilterTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledAccountCannotUsePreviouslyIssuedAccessToken() throws Exception {
        var account = UserAccount.employee(20L, "员工", "13800000000", UserRole.EMPLOYEE);
        account.assignId(7L);
        var repository = new SingleUserRepository(account);
        var sessions = new SessionService(
                new InMemorySessionStore(), Clock.systemUTC(),
                new SessionProperties(Duration.ofHours(2), Duration.ofDays(30)));
        var tokens = sessions.create(new SessionPrincipal(7L, UserRole.EMPLOYEE, 20L, "员工"));
        account.disable();
        var filter = new SessionAuthenticationFilter(sessions, repository);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokens.accessToken());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void enabledAccountKeepsUsingIssuedAccessToken() throws Exception {
        var account = UserAccount.employee(20L, "员工", "13800000000", UserRole.EMPLOYEE);
        account.assignId(7L);
        var repository = new SingleUserRepository(account);
        var sessions = new SessionService(
                new InMemorySessionStore(), Clock.systemUTC(),
                new SessionProperties(Duration.ofHours(2), Duration.ofDays(30)));
        var principal = new SessionPrincipal(7L, UserRole.EMPLOYEE, 20L, "员工");
        var tokens = sessions.create(principal);
        var filter = new SessionAuthenticationFilter(sessions, repository);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokens.accessToken());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
    }

    private record SingleUserRepository(UserAccount account) implements UserAccountRepository {
        @Override public UserAccount save(UserAccount user) { return user; }
        @Override public Optional<UserAccount> findByUsername(String username) { return Optional.empty(); }
        @Override public Optional<UserAccount> findByPhone(String phone) { return Optional.empty(); }
        @Override public Optional<UserAccount> findByOpenid(String openid) { return Optional.empty(); }
        @Override public Optional<UserAccount> findById(Long id) {
            return account.id().equals(id) ? Optional.of(account) : Optional.empty();
        }
    }
}
