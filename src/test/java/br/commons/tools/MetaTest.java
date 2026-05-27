package br.commons.tools;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MetaTest {

    // ── fqn ───────────────────────────────────────────────────────────────────

    @Test
    void fqn_fromObject_returnsFullClassName() {
        assertEquals("java.lang.String", Meta.fqn("hello"));
        assertEquals("java.lang.Integer", Meta.fqn(42));
    }

    @Test
    void fqn_fromClass_returnsFullClassName() {
        assertEquals("java.lang.String", Meta.fqn(String.class));
        assertEquals("java.lang.Integer", Meta.fqn(Integer.class));
    }

    // ── assignable ────────────────────────────────────────────────────────────

    @Test
    void assignable_sameClass() {
        assertTrue(Meta.assignable(String.class, String.class));
    }

    @Test
    void assignable_subclassToSuperclass() {
        assertTrue(Meta.assignable(Integer.class, Number.class));
    }

    @Test
    void assignable_unrelated() {
        assertFalse(Meta.assignable(String.class, Integer.class));
    }

    @Test
    void assignable_nullClass_returnsFalse() {
        assertFalse(Meta.assignable((Class<?>) null, String.class));
        assertFalse(Meta.assignable(String.class, null));
    }

    @Test
    void assignable_nullObject_returnsFalse() {
        assertFalse(Meta.assignable((Object) null, String.class));
    }

    @Test
    void assignable_objectToType_byInstance() {
        assertTrue(Meta.assignable("hello", String.class));
        assertTrue(Meta.assignable(42, Number.class));
    }

    // ── evaluate ──────────────────────────────────────────────────────────────

    @Test
    void evaluate_plainValues_unchanged() {
        Object[] args = {"hello", 42, true};
        Object[] result = Meta.evaluate(args);
        assertArrayEquals(args, result);
    }

    @Test
    void evaluate_supplierArgs_resolved() {
        Supplier<Object> s = () -> "from supplier";
        Object[] result = Meta.evaluate(new Object[]{s, "plain"});
        assertEquals("from supplier", result[0]);
        assertEquals("plain", result[1]);
    }

    @Test
    void evaluate_mixedArgs() {
        Object[] result = Meta.evaluate(new Object[]{(Supplier<Object>) () -> 99, "x"});
        assertEquals(99, result[0]);
        assertEquals("x", result[1]);
    }

    // ── fields ────────────────────────────────────────────────────────────────

    @Test
    void field_existingField_found() {
        TestContainer obj = new TestContainer();
        var field = Meta.field(obj, "value");
        assertNotNull(field);
        assertEquals("value", field.getName());
    }

    @Test
    void field_missingField_returnsNull() {
        assertNull(Meta.field(new TestContainer(), "nonExistent"));
    }

    // ── close ─────────────────────────────────────────────────────────────────

    @Test
    void close_null_noException() {
        assertDoesNotThrow(() -> Meta.close(null));
    }

    @Test
    void close_closeable_callsClose() {
        var closed = new boolean[]{false};
        Meta.close(() -> closed[0] = true);
        assertTrue(closed[0]);
    }

    // helper
    static class TestContainer {
        public String value = "test";
    }
}
