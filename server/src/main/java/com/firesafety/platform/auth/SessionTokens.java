package com.firesafety.platform.auth;

public record SessionTokens(String accessToken, String refreshToken, long expiresInSeconds) {
}
