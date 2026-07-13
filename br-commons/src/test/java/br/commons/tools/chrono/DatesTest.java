package br.commons.tools.chrono;

import br.commons.chrono.Dates;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DatesTest {

    @Test
    void millisInDay_correctValue() {
        assertEquals(86_400_000L, Dates.MILLIS_IN_DAY);
    }

    @Test
    void monthsPtbr_allTwelveMonths() {
        assertEquals(12, Dates.MONTHS_PTBR.size());
    }

    @Test
    void monthsPtbr_correctMappings() {
        assertEquals(1, Dates.MONTHS_PTBR.get("JANEIRO"));
        assertEquals(6, Dates.MONTHS_PTBR.get("JUNHO"));
        assertEquals(12, Dates.MONTHS_PTBR.get("DEZEMBRO"));
    }

    @Test
    void patternDdMmYyyy_matchesValidDate() {
        assertTrue(Dates.PATTERN_DDMMYYYY.matcher("31/12/2024").find());
        assertTrue(Dates.PATTERN_DDMMYYYY.matcher("01/01/2000").find());
    }

    @Test
    void patternDdMmYyyy_noMatchForInvalidFormat() {
        assertFalse(Dates.PATTERN_DDMMYYYY.matcher("2024-12-31").find());
        assertFalse(Dates.PATTERN_DDMMYYYY.matcher("31-12-2024").find());
    }

    @Test
    void formatterDdMmYyyy_parsesDate() {
        LocalDate date = LocalDate.parse("31/12/2024", Dates.FORMATTER_DDMMYYYY);
        assertEquals(31, date.getDayOfMonth());
        assertEquals(12, date.getMonthValue());
        assertEquals(2024, date.getYear());
    }

    @Test
    void formatterDdMmYyyy_formatsDate() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        assertEquals("15/03/2024", date.format(Dates.FORMATTER_DDMMYYYY));
    }
}
