package com.firesafety.platform.auth;

public record SessionPrincipal(Long userId, UserRole role, Long enterpriseId, String displayName) {
}
