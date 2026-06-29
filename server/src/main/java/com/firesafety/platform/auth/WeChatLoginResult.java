package com.firesafety.platform.auth;

public record WeChatLoginResult(
        boolean bindingRequired,
        String bindingTicket,
        SessionPrincipal user,
        SessionTokens tokens) {

    public static WeChatLoginResult bindingRequired(String ticket) {
        return new WeChatLoginResult(true, ticket, null, null);
    }

    public static WeChatLoginResult authenticated(AuthenticationResult result) {
        return new WeChatLoginResult(false, null, result.user(), result.tokens());
    }
}
