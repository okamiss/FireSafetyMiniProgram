package com.firesafety.platform.support;

import com.firesafety.platform.auth.SessionStore;
import com.firesafety.platform.auth.StoredRefreshToken;
import com.firesafety.platform.auth.StoredSession;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemorySessionStore implements SessionStore {
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
