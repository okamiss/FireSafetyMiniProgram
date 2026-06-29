package com.firesafety.platform.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(byte[] content, String originalName, String contentType) {
        var safeOriginalName = safeOriginalName(originalName);
        var extension = extensionOf(safeOriginalName);
        var date = LocalDate.now(ZoneOffset.UTC);
        var storageKey = "%04d/%02d/%02d/%s%s".formatted(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth(), UUID.randomUUID(), extension);
        var destination = root.resolve(storageKey).normalize();
        if (!destination.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store file", exception);
        }
        return new StoredFile(storageKey.replace('\\', '/'), safeOriginalName, contentType, content.length);
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "file";
        }
        var normalized = originalName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String extensionOf(String fileName) {
        var dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        var extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }
}
