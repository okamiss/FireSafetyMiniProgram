package com.firesafety.platform.auth;

import java.time.Instant;

public record StoredSession(SessionPrincipal principal, String refreshToken, Instant createdAt) {
}
