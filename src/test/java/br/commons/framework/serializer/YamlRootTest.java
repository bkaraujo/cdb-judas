package br.commons.framework.serializer;

import br.commons.Yaml;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlRootTest {

    // ── factory ───────────────────────────────────────────────────────────────

    @Test
    void from_emptyMap_isEmpty() {
        Yaml yaml = Yaml.from();
        assertTrue(yaml.isEmpty());
    }

    @Test
    void from_populatedMap_notEmpty() {
        Yaml yaml = Yaml.from(Map.of("key", "value"));
        assertFalse(yaml.isEmpty());
    }

    @Test
    void from_nullPath_isEmpty() {
        Yaml yaml = Yaml.from((java.nio.file.Path) null);
        assertTrue(yaml.isEmpty());
    }

    // ── contains ─────────────────────────────────────────────────────────────

    @Test
    void contains_presentKey() {
        Yaml yaml = Yaml.from(Map.of("host", "localhost"));
        assertTrue(yaml.contains("host"));
    }

    @Test
    void contains_missingKey() {
        Yaml yaml = Yaml.from(Map.of("host", "localhost"));
        assertFalse(yaml.contains("port"));
    }

    @Test
    void contains_nestedKey_withDotNotation() {
        Yaml yaml = Yaml.from(Map.of("server", Map.of("port", 8080)));
        assertTrue(yaml.contains("server.port"));
    }

    // ── asString ─────────────────────────────────────────────────────────────

    @Test
    void asString_presentKey() {
        Yaml yaml = Yaml.from(Map.of("name", "Alice"));
        assertEquals("Alice", yaml.asString("name"));
    }

    @Test
    void asString_missingKey_returnsNull() {
        Yaml yaml = Yaml.from(Map.of());
        assertNull(yaml.asString("name"));
    }

    @Test
    void asString_withDefault_missingKey() {
        Yaml yaml = Yaml.from(Map.of());
        assertEquals("default", yaml.asString("name", "default"));
    }

    @Test
    void asString_withDefault_presentKey() {
        Yaml yaml = Yaml.from(Map.of("name", "Alice"));
        assertEquals("Alice", yaml.asString("name", "default"));
    }

    // ── asInt ────────────────────────────────────────────────────────────────

    @Test
    void asInt_integerValue() {
        Yaml yaml = Yaml.from(Map.of("port", 8080));
        assertEquals(8080, yaml.asInt("port"));
    }

    @Test
    void asInt_stringValue_parsed() {
        Yaml yaml = Yaml.from(Map.of("port", "8080"));
        assertEquals(8080, yaml.asInt("port"));
    }

    @Test
    void asInt_missingKey_returnsNull() {
        assertNull(Yaml.from(Map.of()).asInt("port"));
    }

    @Test
    void asInt_withDefault_missingKey() {
        assertEquals(9090, Yaml.from(Map.of()).asInt("port", 9090));
    }

    // ── asLong ───────────────────────────────────────────────────────────────

    @Test
    void asLong_value() {
        Yaml yaml = Yaml.from(Map.of("ttl", 86400L));
        assertEquals(86400L, yaml.asLong("ttl"));
    }

    @Test
    void asLong_withDefault_missingKey() {
        assertEquals(100L, Yaml.from(Map.of()).asLong("ttl", 100L));
    }

    // ── asDouble ─────────────────────────────────────────────────────────────

    @Test
    void asDouble_value() {
        Yaml yaml = Yaml.from(Map.of("ratio", 0.5));
        assertEquals(0.5, yaml.asDouble("ratio"), 0.001);
    }

    @Test
    void asDouble_withDefault_missingKey() {
        assertEquals(1.0, Yaml.from(Map.of()).asDouble("ratio", 1.0), 0.001);
    }

    // ── asBoolean ────────────────────────────────────────────────────────────

    @Test
    void asBoolean_true() {
        Yaml yaml = Yaml.from(Map.of("enabled", true));
        assertTrue(yaml.asBoolean("enabled"));
    }

    @Test
    void asBoolean_false() {
        Yaml yaml = Yaml.from(Map.of("enabled", false));
        assertFalse(yaml.asBoolean("enabled"));
    }

    @Test
    void asBoolean_withDefault_missingKey() {
        assertTrue(Yaml.from(Map.of()).asBoolean("enabled", true));
    }

    // ── asEnum ───────────────────────────────────────────────────────────────

    @Test
    void asEnum_validValue() {
        Yaml yaml = Yaml.from(Map.of("level", "DEBUG"));
        br.commons.framework.logger.LogLevel level = yaml.asEnum("level", br.commons.framework.logger.LogLevel.class);
        assertEquals(br.commons.framework.logger.LogLevel.DEBUG, level);
    }

    @Test
    void asEnum_missingKey_returnsNull() {
        assertNull(Yaml.from(Map.of()).asEnum("level", br.commons.framework.logger.LogLevel.class));
    }

    @Test
    void asEnum_invalidValue_returnsNull() {
        Yaml yaml = Yaml.from(Map.of("level", "NONEXISTENT"));
        assertNull(yaml.asEnum("level", br.commons.framework.logger.LogLevel.class));
    }

    // ── nested / subtree ─────────────────────────────────────────────────────

    @Test
    void subtree_existingKey_returnsSubYaml() {
        Yaml yaml = Yaml.from(Map.of("db", Map.of("host", "localhost", "port", 5432)));
        Yaml sub = yaml.subtree("db");
        assertNotNull(sub);
        assertEquals("localhost", sub.asString("host"));
        assertEquals(5432, sub.asInt("port"));
    }

    @Test
    void subtree_missingKey_returnsNull() {
        assertNull(Yaml.from(Map.of()).subtree("db"));
    }

    @Test
    void dotNotation_nestedValue() {
        Yaml yaml = Yaml.from(Map.of("server", Map.of("host", "example.com")));
        assertEquals("example.com", yaml.asString("server.host"));
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_missingKey_returnsEmpty() {
        assertTrue(Yaml.from(Map.of()).list("items").isEmpty());
    }

    // ── write operations ──────────────────────────────────────────────────────

    @Test
    void put_topLevelKey() {
        Yaml yaml = Yaml.from();
        yaml.put("name", "Bob");
        assertEquals("Bob", yaml.asString("name"));
    }

    @Test
    void put_nestedKey() {
        Yaml yaml = Yaml.from();
        yaml.put("server.port", "9090");
        assertEquals(9090, yaml.asInt("server.port"));
    }

    @Test
    void append_keyValue() {
        Yaml yaml = Yaml.from();
        yaml.append("env", "production");
        assertEquals("production", yaml.asString("env"));
    }

    @Test
    void clear_removesAllData() {
        // Must use mutable map — Map.of() is immutable
        java.util.Map<String, Object> mutable = new java.util.LinkedHashMap<>();
        mutable.put("key", "value");
        Yaml yaml = Yaml.from(mutable);
        yaml.clear();
        assertTrue(yaml.isEmpty());
    }

    @Test
    void append_yamlEntry() {
        Yaml yaml = Yaml.from();
        yaml.append(new YamlEntry("host", "localhost"));
        assertEquals("localhost", yaml.asString("host"));
    }

    // ── folder ────────────────────────────────────────────────────────────────

    @Test
    void folder_fromMap_returnsNull() {
        assertNull(Yaml.from(Map.of()).folder());
    }
}
