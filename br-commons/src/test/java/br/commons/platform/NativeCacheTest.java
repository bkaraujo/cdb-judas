package br.commons.platform;

import lombok.val;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NativeCacheTest {

    @Test
    void testPutGetRemove() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test:1", 64);
            assertNotNull(seg);
            assertEquals(seg, cache.get("test:1"));

            cache.remove("test:1");
            assertNull(cache.get("test:1"));
        }
    }

    @Test
    void testPutAlwaysReallocates() {
        try (val cache = new NativeCache()) {
            val seg1 = cache.put("key", 64);
            val seg2 = cache.put("key", 64);
            assertNotEquals(seg1, seg2);
        }
    }

    @Test
    void testPutIsZeroed() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            for (long i = 0; i < 64; i++) {
                assertEquals(0, seg.get(ValueLayout.JAVA_BYTE, i));
            }
        }
    }

    @Test
    void testKeys() {
        try (val cache = new NativeCache()) {
            cache.put("TAG:1", 64);
            cache.put("TAG:2", 64);
            cache.put("CAT:1", 64);

            val keys = cache.keys("TAG:");
            assertEquals(2, keys.size());
            assertTrue(keys.contains("TAG:1") && keys.contains("TAG:2"));

            val all = cache.keys("TAG");
            assertEquals(2, all.size());
        }
    }

    @Test
    void testSize() {
        try (val cache = new NativeCache()) {
            assertEquals(0, cache.size());
            cache.put("a", 64);
            assertEquals(1, cache.size());
            cache.put("b", 64);
            assertEquals(2, cache.size());
        }
    }

    @Test
    void testCloseIdempotent() {
        val cache = new NativeCache();
        assertFalse(cache.isClosed());
        cache.close();
        assertTrue(cache.isClosed());
        cache.close();
        assertTrue(cache.isClosed());
    }

    @Test
    void testAccessAfterCloseThrows() {
        val cache = new NativeCache();
        cache.close();
        assertThrows(IllegalStateException.class, () -> cache.put("test", 64));
        assertThrows(IllegalStateException.class, () -> cache.get("test"));
        assertThrows(IllegalStateException.class, () -> cache.remove("test"));
        assertThrows(IllegalStateException.class, () -> cache.keys(""));
        assertThrows(IllegalStateException.class, cache::size);
    }

    // Codec tests

    @Test
    void testUuidCodec() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            val id = UUID.randomUUID();

            NativeCache.writeUuid(seg, 0, id);
            val read = NativeCache.readUuid(seg, 0);
            assertEquals(id, read);
        }
    }

    @Test
    void testUuidNull() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            NativeCache.writeUuid(seg, 0, null);
            assertNull(NativeCache.readUuid(seg, 0));
        }
    }

    @Test
    void testUuidComponents() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            val id = UUID.randomUUID();
            NativeCache.writeUuid(seg, 0, id);

            assertEquals(id.getMostSignificantBits(), NativeCache.readUuidMsb(seg, 0));
            assertEquals(id.getLeastSignificantBits(), NativeCache.readUuidLsb(seg, 0));
        }
    }

    @Test
    void testStringCodec() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 256);
            val str = "hello world";

            NativeCache.writeString(seg, 0, 256, str);
            val read = NativeCache.readString(seg, 0);
            assertEquals(str, read);
        }
    }

    @Test
    void testStringNull() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            NativeCache.writeString(seg, 0, 64, null);
            assertNull(NativeCache.readString(seg, 0));
        }
    }

    @Test
    void testStringEmpty() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            NativeCache.writeString(seg, 0, 64, "");
            assertEquals("", NativeCache.readString(seg, 0));
        }
    }

    @Test
    void testStringUtf8() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 256);
            val str = "olá mundo àáâã";

            NativeCache.writeString(seg, 0, 256, str);
            val read = NativeCache.readString(seg, 0);
            assertEquals(str, read);
        }
    }

    @Test
    void testStringTruncated() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 100);
            val str = "this is a very long string that should definitely be truncated because we only have twenty bytes of capacity";

            NativeCache.writeString(seg, 0, 20, str);
            val read = NativeCache.readString(seg, 0);
            assertNotNull(read);
            assertFalse(read.isEmpty());
            assertTrue(read.length() < str.length());
        }
    }

    @Test
    void testBooleanCodec() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);

            NativeCache.writeBoolean(seg, 0, true);
            assertTrue(NativeCache.readBoolean(seg, 0));

            NativeCache.writeBoolean(seg, 0, false);
            assertFalse(NativeCache.readBoolean(seg, 0));
        }
    }

    @Test
    void testMoneyCodec() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            val amount = new BigDecimal("123.45");

            NativeCache.writeMoney(seg, 0, amount);
            val cents = NativeCache.readMoneyCents(seg, 0);
            assertEquals(12345L, cents);
        }
    }

    @Test
    void testMoneyNull() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            NativeCache.writeMoney(seg, 0, null);
            assertEquals(NativeCache.NULL_LONG, NativeCache.readMoneyCents(seg, 0));
        }
    }

    @Test
    void testMoneyRounding() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            val amount = new BigDecimal("123.456");

            NativeCache.writeMoney(seg, 0, amount);
            val cents = NativeCache.readMoneyCents(seg, 0);
            assertEquals(12346L, cents); // HALF_UP
        }
    }

    @Test
    void testTimestampCodec() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            val ts = LocalDateTime.of(2023, 1, 15, 10, 30, 45);

            NativeCache.writeTimestamp(seg, 0, ts);
            val millis = NativeCache.readTimestampMillis(seg, 0);
            assertTrue(millis > 0);
        }
    }

    @Test
    void testTimestampNull() {
        try (val cache = new NativeCache()) {
            val seg = cache.put("test", 64);
            NativeCache.writeTimestamp(seg, 0, null);
            assertEquals(NativeCache.NULL_LONG, NativeCache.readTimestampMillis(seg, 0));
        }
    }
}
