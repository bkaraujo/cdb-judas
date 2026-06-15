package br.commons.framework.serializer;

import br.commons.Logger;
import br.commons.Yaml;
import br.commons.tools.Tuple;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Yaml aggregator that delegates to specialized reader and writer classes.
 * <p>
 * Reading operations use a FlatMap for O(1) access and an LRU cache.
 * Writing operations use the standard HashMap structure.
 */
@NullMarked
public final class YamlRoot implements Yaml {
    private static final ObjectMapper MAPPER = YAMLMapper.builder().build();

    private final YamlReader reader;
    private final YamlWriter writer;

    // ========== Factory Methods ==========

    private static YamlRoot empty() {
        return from(new LinkedHashMap<>());
    }

    public static YamlRoot from(@Nullable Path path) {
        if (path == null || !Files.exists(path)) {
            return empty();
        }

        try {
            if (Files.size(path) == 0) {
                val yaml = empty();
                yaml.reader.setSource(path);
                yaml.writer.setSource(path);
                return yaml;
            }

            val raw = MAPPER.readValue(path.toFile(), Map.class);
            val yaml = from(raw);
            yaml.reader.setSource(path);
            yaml.writer.setSource(path);
            return yaml;
        } catch (Throwable throwable) {
            Logger.error("Failed to read %s: %s", path, throwable);
            return empty();
        }
    }

    public static YamlRoot from(Map<String, Object> values) {
        return new YamlRoot(values);
    }

    private YamlRoot(Map<String, Object> raw) {
        this.reader = new YamlReader(raw);
        this.writer = new YamlWriter(raw, reader);
    }

    YamlReader reader() { return reader; }
    YamlWriter writer() { return writer; }

    @Override
    public boolean isEmpty() {
        return reader.isEmpty();
    }

    @Override
    @Nullable
    public Path folder() {
        return reader.folder();
    }

    @Override
    public boolean contains(String key) {
        return reader.contains(key);
    }

    @Override @Nullable public Byte    asByte    (String key) { return reader.asByte(key); }
    @Override @Nullable public Short   asShort   (String key) { return reader.asShort(key); }
    @Override @Nullable public Integer asInt     (String key) { return reader.asInt(key); }
    @Override @Nullable public Long    asLong    (String key) { return reader.asLong(key); }
    @Override @Nullable public Float   asFloat   (String key) { return reader.asFloat(key); }
    @Override @Nullable public Double  asDouble  (String key) { return reader.asDouble(key); }
    @Override @Nullable public Boolean asBoolean (String key) { return reader.asBoolean(key); }

    @Override public Byte    asByte    (String key, byte    defaultValue) { val result = asByte(key); return result == null ? defaultValue : result; }
    @Override public Short   asShort   (String key, short   defaultValue) { val result = asShort(key); return result == null ? defaultValue : result; }
    @Override public Integer asInt     (String key, int     defaultValue) { val result = asInt(key); return result == null ? defaultValue : result; }
    @Override public Long    asLong    (String key, long    defaultValue) { val result = asLong(key); return result == null ? defaultValue : result; }
    @Override public Float   asFloat   (String key, float   defaultValue) { val result = asFloat(key); return result == null ? defaultValue : result; }
    @Override public Double  asDouble  (String key, double  defaultValue) { val result = asDouble(key); return result == null ? defaultValue : result; }
    @Override public String  asString  (String key, String defaultValue) { val result = asString(key); return result == null ? defaultValue : result; }
    @Override public Boolean asBoolean (String key, boolean defaultValue) { val result = asBoolean(key); return result == null ? defaultValue : result; }

    @Override public byte[]   asBytes   (String key) { return reader.asBytes(key); }
    @Override public short[]  asShorts  (String key) { return reader.asShorts(key); }
    @Override public int[]    asInts    (String key) { return reader.asInts(key); }
    @Override public long[]   asLongs   (String key) { return reader.asLongs(key); }
    @Override public float[]  asFloats  (String key) { return reader.asFloats(key); }
    @Override public double[] asDoubles (String key) { return reader.asDoubles(key); }

    @Override
    @Nullable
    public String asString(String key) {
        return reader.asString(key);
    }

    @Override
    @Nullable
    public <T extends Enum<T>> T asEnum(String key, Class<T> klass) {
        return reader.asEnum(key, klass);
    }

    @Override
    public Tuple[] asTuples(String key) {
        return reader.asTuples(key);
    }

    @Override
    public List<Yaml> list(String key) {
        return reader.list(key);
    }

    @Override
    @Nullable
    public Yaml subtree(String key) {
        return reader.subtree(key);
    }

    // ========== Delegated Write Operations ==========

    @Override
    public void clear() {
        writer.clear();
    }

    @Override
    public void append(String key, String value) {
        writer.append(key, value);
    }

    @Override
    public void append(YamlEntry... values) {
        writer.append(values);
    }

    @Override
    public void append(String key, String name, Object value) {
        writer.append(key, name, value);
    }

    @Override
    public void append(String key, YamlEntry... values) {
        writer.append(key, values);
    }

    @Override
    public void put(String key, Object value) {
        writer.put(key, value);
    }

    @Override
    public void save() {
        writer.save();
    }

    @Override
    public void save(Path path) {
        writer.save(path);
    }

    // ========== Object Methods ==========

    @Override
    public String toString() {
        val source = reader.source();
        return source == null ? "null" : source.toString();
    }
}
