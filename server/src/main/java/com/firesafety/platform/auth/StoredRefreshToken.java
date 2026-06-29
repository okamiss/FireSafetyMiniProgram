package com.firesafety.platform.auth;

import java.time.Instant;

public record StoredRefreshToken(SessionPrincipal principal, String accessToken, Instant createdAt) {
}
