package br.commons.framework.logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogFilterTest {

    private LogFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LogFilter(LogLevel.INFO);
    }

    // ── global level ──────────────────────────────────────────────────────────

    @Test
    void defaultLevel_isConfigured() {
        assertEquals(LogLevel.INFO, filter.level());
    }

    @Test
    void setLevel_updatesGlobalLevel() {
        filter.level(LogLevel.DEBUG);
        assertEquals(LogLevel.DEBUG, filter.level());
    }

    // ── hasPackageFilters ─────────────────────────────────────────────────────

    @Test
    void hasPackageFilters_falseByDefault() {
        assertFalse(filter.hasPackageFilters());
    }

    @Test
    void hasPackageFilters_trueAfterSet() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        assertTrue(filter.hasPackageFilters());
    }

    @Test
    void hasPackageFilters_falseAfterClear() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        filter.clearPackageFilters();
        assertFalse(filter.hasPackageFilters());
    }

    // ── getPackageLevel ───────────────────────────────────────────────────────

    @Test
    void getPackageLevel_configured() {
        filter.setPackageLevel("br.example", LogLevel.WARN);
        assertEquals(LogLevel.WARN, filter.getPackageLevel("br.example"));
    }

    @Test
    void getPackageLevel_unconfigured_returnsNull() {
        assertNull(filter.getPackageLevel("br.missing"));
    }

    // ── removePackageLevel ────────────────────────────────────────────────────

    @Test
    void removePackageLevel_removesEntry() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        filter.removePackageLevel("br.example");
        assertNull(filter.getPackageLevel("br.example"));
    }

    // ── getEffectiveLevel — hierarchy ─────────────────────────────────────────

    @Test
    void effectiveLevel_exactMatch() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        assertEquals(LogLevel.DEBUG, filter.getEffectiveLevel("br.example"));
    }

    @Test
    void effectiveLevel_inheritsFromParentPackage() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        assertEquals(LogLevel.DEBUG, filter.getEffectiveLevel("br.example.sub.MyClass"));
    }

    @Test
    void effectiveLevel_moreSpecificOverridesParent() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        filter.setPackageLevel("br.example.sub", LogLevel.ERROR);
        assertEquals(LogLevel.ERROR, filter.getEffectiveLevel("br.example.sub.MyClass"));
        assertEquals(LogLevel.DEBUG, filter.getEffectiveLevel("br.example.other.MyClass"));
    }

    @Test
    void effectiveLevel_noMatchFallsBackToGlobal() {
        assertEquals(LogLevel.INFO, filter.getEffectiveLevel("com.unrelated.Class"));
    }

    @Test
    void effectiveLevel_classExactMatch() {
        filter.setPackageLevel("br.example.MyClass", LogLevel.FATAL);
        assertEquals(LogLevel.FATAL, filter.getEffectiveLevel("br.example.MyClass"));
    }

    // ── cache invalidation ────────────────────────────────────────────────────

    @Test
    void cacheInvalidated_whenGlobalLevelChanges() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        assertEquals(LogLevel.DEBUG, filter.getEffectiveLevel("br.example.Cls"));

        filter.level(LogLevel.WARN);
        // Cache should be cleared; global level changed but package filter still exists
        assertEquals(LogLevel.DEBUG, filter.getEffectiveLevel("br.example.Cls"));
    }

    @Test
    void cacheInvalidated_whenPackageFilterSet() {
        assertEquals(LogLevel.INFO, filter.getEffectiveLevel("br.example.Cls"));
        filter.setPackageLevel("br.example", LogLevel.TRACE);
        assertEquals(LogLevel.TRACE, filter.getEffectiveLevel("br.example.Cls"));
    }

    @Test
    void cacheInvalidated_whenPackageFilterRemoved() {
        filter.setPackageLevel("br.example", LogLevel.DEBUG);
        assertEquals(LogLevel.DEBUG, filter.getEffectiveLevel("br.example.Cls"));

        filter.removePackageLevel("br.example");
        assertEquals(LogLevel.INFO, filter.getEffectiveLevel("br.example.Cls"));
    }
}
