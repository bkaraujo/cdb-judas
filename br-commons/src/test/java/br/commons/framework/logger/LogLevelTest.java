package br.commons.framework.logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLevelTest {

    @Test
    void fromSlf4jLevel_trace() {
        assertEquals(LogLevel.TRACE, LogLevel.fromSlf4jLevel("TRACE"));
        assertEquals(LogLevel.TRACE, LogLevel.fromSlf4jLevel("trace"));
    }

    @Test
    void fromSlf4jLevel_debug() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromSlf4jLevel("DEBUG"));
        assertEquals(LogLevel.DEBUG, LogLevel.fromSlf4jLevel("debug"));
    }

    @Test
    void fromSlf4jLevel_warn() {
        assertEquals(LogLevel.WARN, LogLevel.fromSlf4jLevel("WARN"));
        assertEquals(LogLevel.WARN, LogLevel.fromSlf4jLevel("WARNING"));
        assertEquals(LogLevel.WARN, LogLevel.fromSlf4jLevel("warn"));
    }

    @Test
    void fromSlf4jLevel_error() {
        assertEquals(LogLevel.ERROR, LogLevel.fromSlf4jLevel("ERROR"));
        assertEquals(LogLevel.ERROR, LogLevel.fromSlf4jLevel("error"));
    }

    @Test
    void fromSlf4jLevel_off() {
        assertEquals(LogLevel.OFF, LogLevel.fromSlf4jLevel("OFF"));
        assertEquals(LogLevel.OFF, LogLevel.fromSlf4jLevel("off"));
    }

    @Test
    void fromSlf4jLevel_unknownDefaultsToInfo() {
        assertEquals(LogLevel.INFO, LogLevel.fromSlf4jLevel("INFO"));
        assertEquals(LogLevel.INFO, LogLevel.fromSlf4jLevel("UNKNOWN"));
        assertEquals(LogLevel.INFO, LogLevel.fromSlf4jLevel(""));
    }

    @Test
    void ordinalOrder_ascendingByLevel() {
        assertTrue(LogLevel.VERBOSE.ordinal < LogLevel.TRACE.ordinal);
        assertTrue(LogLevel.TRACE.ordinal < LogLevel.DEBUG.ordinal);
        assertTrue(LogLevel.DEBUG.ordinal < LogLevel.INFO.ordinal);
        assertTrue(LogLevel.INFO.ordinal < LogLevel.WARN.ordinal);
        assertTrue(LogLevel.WARN.ordinal < LogLevel.ERROR.ordinal);
        assertTrue(LogLevel.ERROR.ordinal < LogLevel.FATAL.ordinal);
    }

    @Test
    void colored_containsLevelName() {
        assertTrue(LogLevel.INFO.colored.contains("INFO"));
        assertTrue(LogLevel.ERROR.colored.contains("ERROR"));
    }
}
