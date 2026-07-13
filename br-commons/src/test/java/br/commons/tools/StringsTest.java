package br.commons.tools;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest {

    // ── upper ─────────────────────────────────────────────────────────────────

    @Test
    void upper_ascii() {
        assertEquals("HELLO", Strings.upper("hello"));
    }

    @Test
    void upper_unicode() {
        assertEquals("AÇÃO", Strings.upper("ação"));
        assertEquals("ÉXITO", Strings.upper("éxito"));
    }

    @Test
    void upper_empty() {
        assertEquals(Strings.EMPTY, Strings.upper(""));
        assertEquals(Strings.EMPTY, Strings.upper("   "));
    }

    @Test
    void upper_nonStringObject() {
        assertEquals("42", Strings.upper(42));
    }

    // ── lower ─────────────────────────────────────────────────────────────────

    @Test
    void lower_ascii() {
        assertEquals("hello", Strings.lower("HELLO"));
    }

    @Test
    void lower_unicode() {
        assertEquals("ação", Strings.lower("AÇÃO"));
    }

    @Test
    void lower_empty() {
        assertEquals(Strings.EMPTY, Strings.lower(""));
        assertEquals(Strings.EMPTY, Strings.lower("   "));
    }

    // ── isEmpty ───────────────────────────────────────────────────────────────

    @Test
    void isEmpty_blank() {
        assertTrue(Strings.isEmpty("   "));
    }

    @Test
    void isEmpty_empty() {
        assertTrue(Strings.isEmpty(""));
    }

    @Test
    void isEmpty_notEmpty() {
        assertFalse(Strings.isEmpty("hello"));
    }

    // ── isAscii ───────────────────────────────────────────────────────────────

    @Test
    void isAscii_pureAscii() {
        assertTrue(Strings.isAscii("hello"));
        assertTrue(Strings.isAscii("ABC123!@#"));
    }

    @Test
    void isAscii_nonAscii() {
        assertFalse(Strings.isAscii("ação"));
        assertFalse(Strings.isAscii("é"));
    }

    // ── isNumeric ─────────────────────────────────────────────────────────────

    @Test
    void isNumeric_digits() {
        assertTrue(Strings.isNumeric("12345"));
    }

    @Test
    void isNumeric_mixed() {
        assertFalse(Strings.isNumeric("123a"));
        assertFalse(Strings.isNumeric("12.3"));
    }

    // ── isUpperCase / isLowerCase ─────────────────────────────────────────────

    @Test
    void isUpperCase_allUpper() {
        assertTrue(Strings.isUpperCase("ABC"));
    }

    @Test
    void isUpperCase_mixed() {
        assertFalse(Strings.isUpperCase("Abc"));
    }

    @Test
    void isLowerCase_allLower() {
        assertTrue(Strings.isLowerCase("abc"));
    }

    @Test
    void isLowerCase_mixed() {
        assertFalse(Strings.isLowerCase("Abc"));
    }

    // ── onlyNumeric ───────────────────────────────────────────────────────────

    @Test
    void onlyNumeric_stripsNonDigits() {
        assertEquals("12345", Strings.onlyNumeric("123-45"));
        assertEquals("9991234567", Strings.onlyNumeric("(999) 123-4567"));
    }

    @Test
    void onlyNumeric_pureDigits_unchanged() {
        assertEquals("123", Strings.onlyNumeric("123"));
    }

    // ── in ────────────────────────────────────────────────────────────────────

    @Test
    void in_present() {
        assertTrue(Strings.in("b", "a", "b", "c"));
    }

    @Test
    void in_absent() {
        assertFalse(Strings.in("z", "a", "b", "c"));
    }

    // ── containsAny ───────────────────────────────────────────────────────────

    @Test
    void containsAny_oneMatch() {
        assertTrue(Strings.containsAny("hello world", "world", "xyz"));
    }

    @Test
    void containsAny_noMatch() {
        assertFalse(Strings.containsAny("hello world", "foo", "bar"));
    }

    // ── count ─────────────────────────────────────────────────────────────────

    @Test
    void count_char() {
        assertEquals(3, Strings.count("banana", 'a'));
        assertEquals(0, Strings.count("hello", 'z'));
    }

    // ── matches ───────────────────────────────────────────────────────────────

    @Test
    void matches_findsAllGroups() {
        Pattern p = Pattern.compile("\\d+");
        Set<String> result = Strings.matches(p, "abc 123 def 456");
        assertTrue(result.contains("123"));
        assertTrue(result.contains("456"));
    }

    @Test
    void matches_nullText_returnsEmpty() {
        Pattern p = Pattern.compile("\\d+");
        assertTrue(Strings.matches(p, null).isEmpty());
    }

    @Test
    void matches_emptyText_returnsEmpty() {
        Pattern p = Pattern.compile("\\d+");
        assertTrue(Strings.matches(p, "").isEmpty());
    }

    // ── floats(float[]) ───────────────────────────────────────────────────────

    @Test
    void floatsArray_empty() {
        assertEquals(Strings.EMPTY, Strings.floats(new float[]{}));
    }

    @Test
    void floatsArray_singleElement() {
        assertEquals("[1.0]", Strings.floats(new float[]{1.0f}));
    }

    @Test
    void floatsArray_multipleElements() {
        assertEquals("[1.0, 2.5, 3.0]", Strings.floats(new float[]{1.0f, 2.5f, 3.0f}));
    }

    // ── split(String, int) ────────────────────────────────────────────────────

    @Test
    void split_exactChunks() {
        var chunks = Strings.split("abcdef", 2);
        assertEquals(3, chunks.size());
        assertEquals("ab", chunks.get(0));
        assertEquals("cd", chunks.get(1));
        assertEquals("ef", chunks.get(2));
    }

    @Test
    void split_lastChunkSmaller() {
        var chunks = Strings.split("abcde", 2);
        assertEquals(3, chunks.size());
        assertEquals("ab", chunks.get(0));
        assertEquals("cd", chunks.get(1));
        assertEquals("e", chunks.get(2));
    }

    // ── sha256 ────────────────────────────────────────────────────────────────

    @Test
    void sha256_knownHash() {
        // SHA-256 of empty string
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Strings.sha256("")
        );
    }

    @Test
    void sha256_deterministicForSameInput() {
        assertEquals(Strings.sha256("hello"), Strings.sha256("hello"));
    }

    @Test
    void sha256_differentForDifferentInput() {
        assertNotEquals(Strings.sha256("hello"), Strings.sha256("world"));
    }

    @Test
    void sha256_produces64HexChars() {
        assertEquals(64, Strings.sha256("test").length());
    }

    // ── bytes / shorts / ints / longs / floats / doubles (from CSV string) ───

    @Test
    void bytes_parsesCommaSeparated() {
        assertArrayEquals(new byte[]{1, 2, 3}, Strings.bytes("1,2,3"));
    }

    @Test
    void ints_parsesCommaSeparated() {
        assertArrayEquals(new int[]{10, 20, 30}, Strings.ints("10,20,30"));
    }

    @Test
    void longs_parsesCommaSeparated() {
        assertArrayEquals(new long[]{100L, 200L}, Strings.longs("100,200"));
    }

    @Test
    void floats_parsesCommaSeparated() {
        assertArrayEquals(new float[]{1.5f, 2.5f}, Strings.floats("1.5,2.5"), 0.001f);
    }

    @Test
    void doubles_parsesCommaSeparated() {
        assertArrayEquals(new double[]{1.5, 2.5}, Strings.doubles("1.5,2.5"), 0.001);
    }

    @Test
    void shorts_parsesCommaSeparated() {
        assertArrayEquals(new short[]{1, 2}, Strings.shorts("1,2"));
    }

    // ── orEmpty ───────────────────────────────────────────────────────────────

    @Test
    void orEmpty_null_returnsEmpty() {
        assertEquals(Strings.EMPTY, Strings.orEmpty(null));
    }

    @Test
    void orEmpty_nonNull_returnsSame() {
        assertEquals("hello", Strings.orEmpty("hello"));
    }

    // ── read(byte[]) ──────────────────────────────────────────────────────────

    @Test
    void readBytes_utf8() {
        byte[] bytes = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", Strings.read(bytes));
    }
}
