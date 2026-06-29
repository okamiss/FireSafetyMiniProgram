package com.firesafety.platform.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.session")
public record SessionProperties(Duration accessTtl, Duration refreshTtl) {

    public SessionProperties {
        accessTtl = accessTtl == null ? Duration.ofHours(2) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(30) : refreshTtl;
    }
}
