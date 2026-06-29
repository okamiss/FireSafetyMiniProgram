package com.firesafety.platform.file;

public record StoredFile(String storageKey, String originalName, String contentType, long size) {
}
