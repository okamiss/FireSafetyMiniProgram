package com.firesafety.platform.auth;

public record AuthenticationResult(SessionPrincipal user, SessionTokens tokens) {
}
