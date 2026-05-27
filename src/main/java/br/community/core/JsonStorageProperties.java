package br.community.core;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NullMarked
@ConfigurationProperties(prefix = "storage.json")
public record JsonStorageProperties(String path, String backend) {

    public JsonStorageProperties {
        if (backend == null || backend.isBlank()) backend = "local";
    }
}
