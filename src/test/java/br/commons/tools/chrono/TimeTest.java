package br.commons.tools.chrono;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeTest {

    @Test
    void millis_positive() {
        assertTrue(Time.millis() > 0);
    }

    @Test
    void millis_monotonicallyNonDecreasing() throws InterruptedException {
        long t1 = Time.millis();
        Thread.sleep(1);
        long t2 = Time.millis();
        assertTrue(t2 >= t1);
    }

    @Test
    void nanos_positive() {
        assertTrue(Time.nanos() > 0);
    }

    @Test
    void seconds_positive() {
        assertTrue(Time.seconds() > 0);
    }

    @Test
    void seconds_roughlyMatchesMillis() {
        double seconds = Time.seconds();
        long millis = Time.millis();
        assertEquals((double) millis / 1000, seconds, 1.0);
    }

    @Test
    void now_notNull() {
        assertNotNull(Time.now());
    }

    @Test
    void now_isLocalDateTime() {
        assertTrue(Time.now() instanceof LocalDateTime);
    }

    @Test
    void yyyyMMdd_matchesFormat() {
        String date = Time.yyyyMMdd();
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"), "Expected yyyy-MM-dd but got: " + date);
    }

    @Test
    void yyyyMMdd_matchesCurrentYear() {
        String date = Time.yyyyMMdd();
        int currentYear = LocalDateTime.now().getYear();
        assertTrue(date.startsWith(String.valueOf(currentYear)));
    }
}
