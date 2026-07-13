package br.commons.framework.logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MDCTest {

    @AfterEach
    void cleanup() {
        MDC.pop("requestId");
        MDC.pop("userId");
        MDC.pop("key");
        MDC.pop("a");
        MDC.pop("b");
    }

    @Test
    void prefix_emptyByDefault() {
        assertEquals("", MDC.prefix());
    }

    @Test
    void push_singleEntry_formatsPrefix() {
        MDC.push("requestId", "abc-123");
        String prefix = MDC.prefix();
        assertEquals("[requestId=abc-123]", prefix);
    }

    @Test
    void push_multipleEntries_allIncluded() {
        MDC.push("a", "1");
        MDC.push("b", "2");
        String prefix = MDC.prefix();
        assertTrue(prefix.contains("a=1"));
        assertTrue(prefix.contains("b=2"));
        assertTrue(prefix.startsWith("["));
        assertTrue(prefix.endsWith("]"));
    }

    @Test
    void pop_removesEntry() {
        MDC.push("key", "value");
        MDC.pop("key");
        assertEquals("", MDC.prefix());
    }

    @Test
    void pop_nonExistent_noException() {
        assertDoesNotThrow(() -> MDC.pop("nonExistent"));
    }

    @Test
    void push_overwritesSameKey() {
        MDC.push("key", "first");
        MDC.push("key", "second");
        assertTrue(MDC.prefix().contains("key=second"));
        assertFalse(MDC.prefix().contains("key=first"));
        MDC.pop("key");
    }
}
