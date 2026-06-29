package com.firesafety.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.common.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

    @Test
    void createsResolvesRefreshesAndRevokesOpaqueSessions() {
        var store = new InMemorySessionStore();
        var clock = Clock.fixed(Instant.parse("2026-06-29T10:00:00Z"), ZoneOffset.UTC);
        var service = new SessionService(store, clock, Duration.ofHours(2), Duration.ofDays(30));
        var principal = new SessionPrincipal(7L, UserRole.SUPER_ADMIN, null, "admin");

        var tokens = service.create(principal);

        assertThat(tokens.accessToken()).doesNotContain(".").hasSizeGreaterThanOrEqualTo(40);
        assertThat(service.resolve(tokens.accessToken())).contains(principal);

        var refreshed = service.refresh(tokens.refreshToken());
        assertThat(refreshed.accessToken()).isNotEqualTo(tokens.accessToken());
        assertThat(service.resolve(tokens.accessToken())).isEmpty();
        assertThat(service.resolve(refreshed.accessToken())).contains(principal);

        service.logout(refreshed.accessToken());
        assertThat(service.resolve(refreshed.accessToken())).isEmpty();
    }

    @Test
    void rejectsUnknownRefreshTokenAsExpiredSession() {
        var service = new SessionService(
                new InMemorySessionStore(), Clock.systemUTC(), Duration.ofHours(2), Duration.ofDays(30));

        assertThatThrownBy(() -> service.refresh("unknown"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("SESSION_EXPIRED"));
    }

    private static final class InMemorySessionStore implements SessionStore {
        private final Map<String, StoredSession> access = new HashMap<>();
        private final Map<String, StoredRefreshToken> refresh = new HashMap<>();

        @Override
        public void saveAccess(String token, StoredSession session, Duration ttl) {
            access.put(token, session);
        }

        @Override
        public Optional<StoredSession> findAccess(String token) {
            return Optional.ofNullable(access.get(token));
        }

        @Override
        public void deleteAccess(String token) {
            access.remove(token);
        }

        @Override
        public void saveRefresh(String token, StoredRefreshToken value, Duration ttl) {
            refresh.put(token, value);
        }

        @Override
        public Optional<StoredRefreshToken> findRefresh(String token) {
            return Optional.ofNullable(refresh.get(token));
        }

        @Override
        public void deleteRefresh(String token) {
            refresh.remove(token);
        }
    }
}
