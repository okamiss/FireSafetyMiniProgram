package com.firesafety.platform.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperties(String localRoot) {

    public StorageProperties {
        localRoot = localRoot == null || localRoot.isBlank() ? "./data/files" : localRoot;
    }
}
