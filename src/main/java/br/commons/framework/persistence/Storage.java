package br.commons.framework.persistence;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public interface Storage {

    byte @Nullable [] read(String file, String jsonKey);

    void write(String file, String jsonKey, byte[] content);

    boolean exists(String file);

    List<String> listFiles(String prefix, String suffix);
}
