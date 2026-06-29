package com.firesafety.platform.auth;

public interface WeChatIdentityProvider {
    String exchangeLoginCode(String loginCode);
    String exchangePhoneCode(String phoneCode);
}
