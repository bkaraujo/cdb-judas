package br.community.core;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ConfigMapping(prefix = "storage.json")
public interface JsonStorageProperties {

    String path();

    @WithDefault("local")
    String backend();
}
