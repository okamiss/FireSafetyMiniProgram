package com.firesafety.platform.file;

import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfiguration {

    @Bean
    FileStorage fileStorage(StorageProperties properties) {
        return new LocalFileStorage(Path.of(properties.localRoot()));
    }
}
