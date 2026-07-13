package br.commons;

import br.commons.framework.serializer.YamlEntry;
import br.commons.framework.serializer.YamlRoot;
import br.commons.tools.Tuple;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public interface Yaml {

    static Yaml from(Path path) { return YamlRoot.from(path); }
    static Yaml from(Map<String, Object> map) { return YamlRoot.from(map); }
    static Yaml from() { return from(new LinkedHashMap<>()); }

    byte[]      asBytes   (String key);
    short[]     asShorts  (String key);
    int[]       asInts    (String key);
    long[]      asLongs   (String key);
    float[]     asFloats  (String key);
    double[]    asDoubles (String key);
    Tuple[]     asTuples  (String key);


    Byte    asByte    (String key, byte defaultValue);
    Short   asShort   (String key, short defaultValue);
    Integer asInt     (String key, int defaultValue);
    Long    asLong    (String key, long defaultValue);
    Float   asFloat   (String key, float defaultValue);
    Boolean asBoolean (String key, boolean defaultValue);
    Double  asDouble  (String key, double defaultValue);
    String  asString  (String key, String defaultValue);

    @Nullable Byte    asByte    (String key);
    @Nullable Short   asShort   (String key);
    @Nullable Integer asInt     (String key);
    @Nullable Long    asLong    (String key);
    @Nullable Float   asFloat   (String key);
    @Nullable Boolean asBoolean (String key);
    @Nullable Double  asDouble  (String key);
    @Nullable String  asString  (String key);
    @Nullable <T extends Enum<T>> T asEnum(String key, Class<T> klass);
    @Nullable Yaml subtree(String key);

    @Nullable Path folder();
    boolean isEmpty();
    boolean contains(String key);
    List<Yaml> list(String key);

    void clear();
    void save();
    void save(Path path);
    void put(String key, Object value);
    void append(String key, String value);
    void append(YamlEntry... values);
    void append(String key, String name, Object value);
    void append(String key, YamlEntry... values);

}
