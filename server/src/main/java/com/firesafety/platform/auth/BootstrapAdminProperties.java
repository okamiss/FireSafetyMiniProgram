package com.firesafety.platform.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bootstrap-admin")
public record BootstrapAdminProperties(String username, String password, String displayName) {
}
