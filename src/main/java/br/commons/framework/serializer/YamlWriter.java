package br.commons.framework.serializer;

import br.commons.Logger;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NullMarked
final class YamlWriter {
    private static final ObjectMapper MAPPER = YAMLMapper.builder().build();

    private final Map<String, Object> raw;
    private final YamlReader reader;
    private @Nullable Path source;

    YamlWriter(Map<String, Object> raw, YamlReader reader) {
        this.raw = raw;
        this.reader = reader;
    }

    void setSource(@Nullable Path source) {
        this.source = source;
    }

    public void clear() {
        raw.clear();
        reader.markDirty();
    }

    public void append(String key, String value) {
        append(new YamlEntry(key, value));
    }

    public void append(YamlEntry... values) {
        for (val entry : values) {
            raw.putAll(entry.toMap());
        }
        reader.markDirty();
    }

    public void append(String key, String name, Object value) {
        append(key, new YamlEntry(name, value));
    }

    @SuppressWarnings("unchecked")
    public void append(String key, YamlEntry... values) {
        var container = YamlNavigator.findInRaw(raw, key);
        if (container instanceof List<?> existingList) {
            val list = (List<Object>) existingList;
            for (val entry : values) {
                list.add(entry.toMap());
            }
            reader.markDirty();
            return;
        }

        val list = new ArrayList<>();
        for (val entry : values) {
            list.add(entry.toMap());
        }

        val dotIndex = key.lastIndexOf('.');
        if (dotIndex > 0) {
            val path = key.substring(0, dotIndex);
            val item = key.substring(dotIndex + 1);
            newMap(path).put(item, list);
        } else {
            newList(key).addAll(list);
        }
        reader.markDirty();
    }

    @SuppressWarnings("unchecked")
    public void put(String key, Object value) {
        val dotIndex = key.lastIndexOf('.');
        if (dotIndex < 0) {
            raw.put(key, value);
            reader.markDirty();
            return;
        }

        val path = key.substring(0, dotIndex);
        val item = key.substring(dotIndex + 1);

        var container = YamlNavigator.findInRaw(raw, path);
        if (container == null) {
            container = newMap(path);
        }

        ((Map<String, Object>) container).put(item, value.toString());
        reader.markDirty();
    }

    public void save() {
        if (source == null) {
            Logger.warn("Failed to save: Yaml not loaded from a known file");
            return;
        }
        save(source);
    }

    public void save(Path path) {
        val file = path.toFile();
        try {
            if (!file.exists()) {
                Files.createFile(path);
            }
            Logger.debug("Writing %s", file);
            MAPPER.writeValue(file, raw);
        } catch (Throwable throwable) {
            Logger.error("Failed to write %s: %s", path, throwable);
        }
    }

    // ========== Internal Methods ==========

    @SuppressWarnings("unchecked")
    private Map<String, Object> newMap(String key) {
        val tokens = YamlNavigator.splitKey(key);

        Object container = raw;
        for (val token : tokens) {
            if (container instanceof Map<?, ?> m) {
                val map = (Map<String, Object>) m;
                val existing = map.get(token);
                if (existing != null) {
                    container = existing;
                } else {
                    container = new LinkedHashMap<String, Object>();
                    map.put(token, container);
                }
                continue;
            }

            if (container instanceof List<?> l) {
                val list = (List<Object>) l;
                container = new LinkedHashMap<String, Object>();
                list.add(container);
            }
        }

        return (Map<String, Object>) container;
    }

    @SuppressWarnings("unchecked")
    private List<Object> newList(String key) {
        val tokens = YamlNavigator.splitKey(key);

        Object container = raw;
        for (int i = 0; i < tokens.length; ++i) {
            val token = tokens[i];

            if (container instanceof Map<?, ?> m) {
                val map = (Map<String, Object>) m;
                if (i != tokens.length - 1) {
                    container = map.get(token);
                } else {
                    container = new ArrayList<>();
                    map.put(token, container);
                }
                continue;
            }

            if (container instanceof List<?> l) {
                val list = (List<Object>) l;
                container = new LinkedHashMap<String, Object>();
                list.add(container);
            }
        }

        return (List<Object>) (container != null ? container : new ArrayList<>());
    }
}
