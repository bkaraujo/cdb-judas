package br.commons.framework.serializer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class YamlEntryTest {

    @Test
    void constructor_storesKeyAndValue() {
        YamlEntry entry = new YamlEntry("myKey", "myValue");
        assertEquals("myKey", entry.key());
        assertEquals("myValue", entry.value());
    }

    @Test
    void toMap_containsKeyValuePair() {
        YamlEntry entry = new YamlEntry("host", "localhost");
        Map<String, Object> map = entry.toMap();
        assertEquals(1, map.size());
        assertEquals("localhost", map.get("host"));
    }

    @Test
    void toMap_numericValue() {
        YamlEntry entry = new YamlEntry("port", 8080);
        Map<String, Object> map = entry.toMap();
        assertEquals(8080, map.get("port"));
    }

    @Test
    void equality_sameFields() {
        YamlEntry a = new YamlEntry("k", "v");
        YamlEntry b = new YamlEntry("k", "v");
        assertEquals(a, b);
    }

    @Test
    void equality_differentKey() {
        assertNotEquals(new YamlEntry("k1", "v"), new YamlEntry("k2", "v"));
    }
}
