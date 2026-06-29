package com.firesafety.platform.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWeChatBindingTicketStore implements WeChatBindingTicketStore {
    private static final String KEY_PREFIX = "auth:wechat-binding:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public RedisWeChatBindingTicketStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String create(String openid) {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        var ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(KEY_PREFIX + ticket, openid, TTL);
        return ticket;
    }

    @Override
    public Optional<String> consume(String ticket) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(KEY_PREFIX + ticket));
    }
}
