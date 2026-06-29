package com.firesafety.platform.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSessionStore implements SessionStore {
    private static final String ACCESS_PREFIX = "session:access:";
    private static final String REFRESH_PREFIX = "session:refresh:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisSessionStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveAccess(String token, StoredSession session, Duration ttl) {
        save(ACCESS_PREFIX + token, session, ttl);
    }

    @Override
    public Optional<StoredSession> findAccess(String token) {
        return find(ACCESS_PREFIX + token, StoredSession.class);
    }

    @Override
    public void deleteAccess(String token) {
        redis.delete(ACCESS_PREFIX + token);
    }

    @Override
    public void saveRefresh(String token, StoredRefreshToken value, Duration ttl) {
        save(REFRESH_PREFIX + token, value, ttl);
    }

    @Override
    public Optional<StoredRefreshToken> findRefresh(String token) {
        return find(REFRESH_PREFIX + token, StoredRefreshToken.class);
    }

    @Override
    public void deleteRefresh(String token) {
        redis.delete(REFRESH_PREFIX + token);
    }

    private void save(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize session", exception);
        }
    }

    private <T> Optional<T> find(String key, Class<T> type) {
        var value = redis.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException exception) {
            redis.delete(key);
            return Optional.empty();
        }
    }
}
