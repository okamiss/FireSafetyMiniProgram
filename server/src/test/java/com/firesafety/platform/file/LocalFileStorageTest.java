package com.firesafety.platform.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    java.nio.file.Path root;

    @Test
    void storesContentUnderConfiguredRootWithUnpredictableName() throws Exception {
        var storage = new LocalFileStorage(root);

        var stored = storage.store("proof".getBytes(StandardCharsets.UTF_8), "../现场照片.jpg", "image/jpeg");

        var absolute = root.resolve(stored.storageKey()).normalize();
        assertThat(absolute).startsWith(root);
        assertThat(absolute.getFileName().toString()).doesNotContain("现场照片").endsWith(".jpg");
        assertThat(Files.readString(absolute)).isEqualTo("proof");
        assertThat(stored.originalName()).isEqualTo("现场照片.jpg");
        assertThat(stored.size()).isEqualTo(5);
    }

    @Test
    void loadsPreviouslyStoredContent() {
        var storage = new LocalFileStorage(root);
        var content = "proof".getBytes(StandardCharsets.UTF_8);

        var stored = storage.store(content, "现场照片.jpg", "image/jpeg");

        assertThat(storage.load(stored.storageKey())).isEqualTo(content);
    }

    @Test
    void rejectsStorageKeyOutsideConfiguredRoot() {
        var storage = new LocalFileStorage(root);

        assertThatThrownBy(() -> storage.load("../outside.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid storage path");
    }
}
