package br.commons.framework.serializer;

import br.commons.Logger;
import br.commons.Yaml;
import br.commons.tools.Strings;
import br.commons.tools.Tuple;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

@NullMarked
final class YamlReader {
    private static final int LRU_CACHE_SIZE = 20;
    private static final Object NULL_SENTINEL = new Object();

    private final Map<String, Object> raw;
    private final Map<String, Object> flatMap;
    private final LinkedHashMap<String, Object> cache;
    private @Nullable Path source;
    private boolean dirty;

    YamlReader(Map<String, Object> raw) {
        this.raw = raw;
        this.flatMap = new LinkedHashMap<>();
        this.cache = new LinkedHashMap<>(LRU_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return size() > LRU_CACHE_SIZE;
            }
        };

        dirty = false;
        flatten(Strings.EMPTY, raw);
    }

    void setSource(Path source) {
        this.source = source;
    }

    Path source() {
        if (source == null) { throw new IllegalStateException("Source not set"); }
        return source;
    }

    Map<String, Object> getRaw() {
        return raw;
    }

    public boolean isEmpty() {
        return raw.isEmpty();
    }

    @Nullable
    public Path folder() {
        return source != null ? source.getParent() : null;
    }

    public boolean contains(String key) {
        ensureFlatMapUpdated();
        return flatMap.containsKey(key) || YamlNavigator.findInRaw(raw, key) != null;
    }

    // ========== Typed Accessors ==========

    @Nullable public Byte    asByte    (String key) { return findTyped(key, Byte.class); }
    @Nullable public Short   asShort   (String key) { return findTyped(key, Short.class); }
    @Nullable public Integer asInt     (String key) { return findTyped(key, Integer.class); }
    @Nullable public Long    asLong    (String key) { return findTyped(key, Long.class); }
    @Nullable public Float   asFloat   (String key) { return findTyped(key, Float.class); }
    @Nullable public Boolean asBoolean (String key) { return findTyped(key, Boolean.class); }
    @Nullable public Double  asDouble  (String key) { return findTyped(key, Double.class); }

    public byte[]   asBytes   (String key) { return Strings.bytes(asString(key)); }
    public short[]  asShorts  (String key) { return Strings.shorts(asString(key)); }
    public int[]    asInts    (String key) { return Strings.ints(asString(key)); }
    public long[]   asLongs   (String key) { return Strings.longs(asString(key)); }
    public float[]  asFloats  (String key) { return Strings.floats(asString(key)); }
    public double[] asDoubles (String key) { return Strings.doubles(asString(key)); }

    @Nullable
    public String asString(String key) {
        val value = findTyped(key, String.class);
        return value != null ? value.trim() : null;
    }

    @Nullable
    public <T extends Enum<T>> T asEnum(String key, Class<T> klass) {
        val value = asString(key);
        if (value == null) return null;

        for (val constant : klass.getEnumConstants()) {
            if (constant.name().equals(value)) {
                return constant;
            }
        }
        return null;
    }

    public Tuple[] asTuples(String key) {
        val properties = new HashMap<String, String>();
        if (contains(key)) {
            for (val property : list(key)) {

                val name = property.asString("name");
                if (name == null) continue;

                val value = property.asString("value");
                if (value == null) continue;

                if (properties.containsKey(name)) {
                    Logger.warn("Duplicate property `" + name + "` in `" + key + "`");
                    continue;
                }
                properties.put(name, value);
            }
        }
        return properties.entrySet().stream()
                .map(v -> new Tuple(v.getKey(), v.getValue()))
                .toList()
                .toArray(new Tuple[0]);
    }

    @SuppressWarnings("unchecked")
    public List<Yaml> list(String key) {
        val result = new ArrayList<Yaml>();
        val dotIndex = key.lastIndexOf('.');

        if (Strings.isNumeric(key.substring(dotIndex + 1))) {
            Logger.warn("Key must relate to a yaml array");
            return result;
        }

        val found = YamlNavigator.findInRaw(raw, key);
        if (!(found instanceof List<?> entries)) {
            Logger.warn("Key must relate to a yaml array");
            return result;
        }

        for (val entry : entries) {
            val yaml = YamlRoot.from((Map<String, Object>) entry);
            yaml.reader().setSource(source());
            result.add(yaml);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public YamlRoot subtree(String key) {
        val result = YamlNavigator.findInRaw(raw, key);
        if (result == null) return null;

        if (result instanceof Map<?, ?> map) {
            return YamlRoot.from((Map<String, Object>) map);
        }

        if (result instanceof List<?>) {
            val map = new LinkedHashMap<String, Object>();
            val dotIndex = key.lastIndexOf('.');
            val desired = dotIndex > 0 ? key.substring(dotIndex + 1) : key;
            map.put(desired, result);
            val yaml = YamlRoot.from(map);
            yaml.reader().setSource(source());
            return yaml;
        }

        return null;
    }

    // ========== Internal Methods ==========

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> map) {
        for (val entry : map.entrySet()) {
            val key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            val value = entry.getValue();

            if (value instanceof Map<?, ?> nested) {
                flatten(key, (Map<String, Object>) nested);
            } else if (value instanceof List<?> list) {
                flatMap.put(key, value);
                for (int i = 0; i < list.size(); i++) {
                    val item = list.get(i);
                    if (item instanceof Map<?, ?> nestedMap) {
                        flatten(key + "." + i, (Map<String, Object>) nestedMap);
                    } else {
                        flatMap.put(key + "." + i, item);
                    }
                }
            } else {
                flatMap.put(key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T findTyped(String key, Class<T> type) {
        ensureFlatMapUpdated();

        // Try flatMap first (O(1) lookup) - before building cache key
        var value = flatMap.get(key);

        // Fallback to raw navigation if not in flatMap
        if (value == null && !flatMap.containsKey(key)) {
            value = YamlNavigator.findInRaw(raw, key);
        }

        if (value == null) return null;

        // Now build cache key and check cache (single lookup with get)
        val cacheKey = key + ":" + type.getSimpleName();
        val cached = cache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_SENTINEL ? null : (T) cached;
        }

        // Convert and cache
        val result = convertValue(value, type);
        cache.put(cacheKey, result != null ? result : NULL_SENTINEL);
        return result;
    }

    @Nullable
    private <T> T convertValue(Object value, Class<T> type) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        if (Number.class.isAssignableFrom(type)) {
            return convertNumber(value, type);
        }

        if (type == Boolean.class) {
            if (value instanceof Boolean b) return type.cast(b);
            return type.cast(Boolean.parseBoolean(value.toString()));
        }

        if (type == String.class) {
            return type.cast(value.toString());
        }

        return type.cast(value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T convertNumber(Object value, Class<T> type) {
        if (value instanceof Number num) {
            val converted = fromNumber(num, type);
            if (converted != null) {
                return (T) converted;
            }
        }

        try {
            val parsed = parseNumber(value.toString(), type);
            return parsed != null ? (T) parsed : null;
        } catch (NumberFormatException ignored) {
            Logger.fatal("Expected a number, got %s", value);
        }

        return null;
    }

    /** Reapresenta um {@link Number} no tipo numérico alvo; {@code null} se o tipo não for suportado. */
    @Nullable
    private static Number fromNumber(Number num, Class<?> type) {
        if (type == Byte.class) return num.byteValue();
        if (type == Short.class) return num.shortValue();
        if (type == Integer.class) return num.intValue();
        if (type == Long.class) return num.longValue();
        if (type == Float.class) return num.floatValue();
        if (type == Double.class) return num.doubleValue();
        return null;
    }

    /** Faz parse da string no tipo numérico alvo; {@code null} se o tipo não for suportado. */
    @Nullable
    private static Number parseNumber(String str, Class<?> type) {
        if (type == Byte.class) return Byte.valueOf(str);
        if (type == Short.class) return Short.valueOf(str);
        if (type == Integer.class) return Integer.valueOf(str);
        if (type == Long.class) return Long.valueOf(str);
        if (type == Float.class) return Float.valueOf(str);
        if (type == Double.class) return Double.valueOf(str);
        return null;
    }

    void markDirty() {
        dirty = true;
        cache.clear();
    }

    private void ensureFlatMapUpdated() {
        if (dirty) {
            flatMap.clear();
            flatten(Strings.EMPTY, raw);
            dirty = false;
        }
    }
}
