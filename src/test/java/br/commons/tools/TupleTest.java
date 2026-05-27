package br.commons.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TupleTest {

    @Test
    void constructor_storesNameAndValue() {
        Tuple t = new Tuple("key", "val");
        assertEquals("key", t.name());
        assertEquals("val", t.value());
    }

    @Test
    void equality_sameFields() {
        Tuple a = new Tuple("k", "v");
        Tuple b = new Tuple("k", "v");
        assertEquals(a, b);
    }

    @Test
    void equality_differentFields() {
        assertNotEquals(new Tuple("k1", "v"), new Tuple("k2", "v"));
        assertNotEquals(new Tuple("k", "v1"), new Tuple("k", "v2"));
    }

    @Test
    void hashCode_sameForEqualTuples() {
        Tuple a = new Tuple("k", "v");
        Tuple b = new Tuple("k", "v");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsFields() {
        String s = new Tuple("myKey", "myVal").toString();
        assertTrue(s.contains("myKey"));
        assertTrue(s.contains("myVal"));
    }
}
