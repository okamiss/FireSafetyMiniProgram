package com.firesafety.platform.auth;

import java.time.Duration;
import java.util.Optional;

public interface SessionStore {
    void saveAccess(String token, StoredSession session, Duration ttl);

    Optional<StoredSession> findAccess(String token);

    void deleteAccess(String token);

    void saveRefresh(String token, StoredRefreshToken value, Duration ttl);

    Optional<StoredRefreshToken> findRefresh(String token);

    void deleteRefresh(String token);
}
