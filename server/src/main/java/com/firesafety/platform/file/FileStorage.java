package com.firesafety.platform.file;

public interface FileStorage {
    StoredFile store(byte[] content, String originalName, String contentType);
}
