package com.firesafety.platform.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.wechat")
public record WeChatProperties(String appId, String appSecret, boolean mockEnabled) {
}
