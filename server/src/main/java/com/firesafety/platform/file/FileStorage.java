package com.firesafety.platform.file;

public interface FileStorage {
    StoredFile store(byte[] content, String originalName, String contentType);

    default byte[] load(String storageKey) {
        throw new UnsupportedOperationException("File loading is not supported");
    }
}
