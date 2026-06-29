package com.firesafety.platform.auth;

import com.firesafety.platform.common.BusinessException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SessionStore store;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    @Autowired
    public SessionService(SessionStore store, Clock clock, SessionProperties properties) {
        this(store, clock, properties.accessTtl(), properties.refreshTtl());
    }

    SessionService(SessionStore store, Clock clock, Duration accessTtl, Duration refreshTtl) {
        this.store = store;
        this.clock = clock;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public SessionTokens create(SessionPrincipal principal) {
        var accessToken = newToken(32);
        var refreshToken = newToken(48);
        var now = clock.instant();
        store.saveAccess(accessToken, new StoredSession(principal, refreshToken, now), accessTtl);
        store.saveRefresh(refreshToken, new StoredRefreshToken(principal, accessToken, now), refreshTtl);
        return new SessionTokens(accessToken, refreshToken, accessTtl.toSeconds());
    }

    public Optional<SessionPrincipal> resolve(String accessToken) {
        return store.findAccess(accessToken).map(StoredSession::principal);
    }

    public SessionTokens refresh(String refreshToken) {
        var stored = store.findRefresh(refreshToken)
                .orElseThrow(() -> new BusinessException(
                        "SESSION_EXPIRED", "登录状态已失效", HttpStatus.UNAUTHORIZED));
        store.deleteAccess(stored.accessToken());
        store.deleteRefresh(refreshToken);
        return create(stored.principal());
    }

    public void logout(String accessToken) {
        store.findAccess(accessToken).ifPresent(session -> store.deleteRefresh(session.refreshToken()));
        store.deleteAccess(accessToken);
    }

    private String newToken(int byteCount) {
        var bytes = new byte[byteCount];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
