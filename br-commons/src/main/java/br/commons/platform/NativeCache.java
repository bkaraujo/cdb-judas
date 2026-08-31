package br.commons.platform;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@NullMarked
public class NativeCache implements AutoCloseable {
    private static final Logger log = Logger.getLogger(NativeCache.class.getName());
    private static final LocalDateTime EPOCH_UTC = LocalDateTime.of(1970, 1, 1, 0, 0);

    public static final long NULL_LONG = Long.MIN_VALUE;
    public static final int  NULL_INT  = Integer.MIN_VALUE;

    private final Arena arena;
    private final Map<String, MemorySegment> index = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public NativeCache() {
        arena = Arena.ofShared();
    }

    public MemorySegment put(String key, long size) {
        if (closed) throw new IllegalStateException("NativeCache is closed");
        val segment = arena.allocate(size);
        MemorySegment.copy(MemorySegment.ofArray(new byte[(int) size]), 0, segment, 0, size);
        index.put(key, segment);
        return segment;
    }

    public @Nullable MemorySegment get(String key) {
        if (closed) throw new IllegalStateException("NativeCache is closed");
        return index.get(key);
    }

    public void remove(String key) {
        if (closed) throw new IllegalStateException("NativeCache is closed");
        index.remove(key);
    }

    public List<String> keys(String prefix) {
        if (closed) throw new IllegalStateException("NativeCache is closed");
        val result = new ArrayList<String>();
        for (val key : index.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return result;
    }

    public int size() {
        if (closed) throw new IllegalStateException("NativeCache is closed");
        return index.size();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        arena.close();
        index.clear();
    }

    // Codecs

    public static void writeUuid(MemorySegment s, long off, @Nullable UUID v) {
        if (v == null) {
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, 0L);
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off + 8, 0L);
        } else {
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, v.getMostSignificantBits());
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off + 8, v.getLeastSignificantBits());
        }
    }

    public static long readUuidMsb(MemorySegment s, long off) {
        return s.get(ValueLayout.JAVA_LONG_UNALIGNED, off);
    }

    public static long readUuidLsb(MemorySegment s, long off) {
        return s.get(ValueLayout.JAVA_LONG_UNALIGNED, off + 8);
    }

    public static @Nullable UUID readUuid(MemorySegment s, long off) {
        val msb = s.get(ValueLayout.JAVA_LONG_UNALIGNED, off);
        val lsb = s.get(ValueLayout.JAVA_LONG_UNALIGNED, off + 8);
        if (msb == 0 && lsb == 0) return null;
        return new UUID(msb, lsb);
    }

    public static void writeString(MemorySegment s, long off, int capacity, @Nullable String v) {
        if (v == null) {
            s.set(ValueLayout.JAVA_INT_UNALIGNED, off, -1);
        } else if (v.isEmpty()) {
            s.set(ValueLayout.JAVA_INT_UNALIGNED, off, 0);
        } else {
            val bytes = v.getBytes(StandardCharsets.UTF_8);
            int len = bytes.length;
            int available = capacity - 4;
            if (len > available) {
                len = truncateUtf8(bytes, available);
                log.warning("String truncated from " + bytes.length + " to " + len + " bytes");
            }
            s.set(ValueLayout.JAVA_INT_UNALIGNED, off, len);
            s.asSlice(off + 4, len).copyFrom(MemorySegment.ofArray(bytes).asSlice(0, len));
        }
    }

    public static @Nullable String readString(MemorySegment s, long off) {
        val len = s.get(ValueLayout.JAVA_INT_UNALIGNED, off);
        if (len < 0) return null;
        if (len == 0) return "";
        val bytes = new byte[len];
        MemorySegment.copy(s, off + 4, MemorySegment.ofArray(bytes), 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeBoolean(MemorySegment s, long off, boolean v) {
        s.set(ValueLayout.JAVA_BYTE, off, v ? (byte) 1 : (byte) 0);
    }

    public static boolean readBoolean(MemorySegment s, long off) {
        return s.get(ValueLayout.JAVA_BYTE, off) != 0;
    }

    public static void writeMoney(MemorySegment s, long off, @Nullable BigDecimal v) {
        if (v == null) {
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, NULL_LONG);
        } else {
            try {
                val cents = v.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
                s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, cents);
            } catch (ArithmeticException e) {
                log.warning("BigDecimal overflow: " + v);
                s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, NULL_LONG);
            }
        }
    }

    public static long readMoneyCents(MemorySegment s, long off) {
        return s.get(ValueLayout.JAVA_LONG_UNALIGNED, off);
    }

    public static void writeTimestamp(MemorySegment s, long off, @Nullable LocalDateTime v) {
        if (v == null) {
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, NULL_LONG);
        } else {
            val millis = ChronoUnit.MILLIS.between(EPOCH_UTC, v);
            s.set(ValueLayout.JAVA_LONG_UNALIGNED, off, millis);
        }
    }

    public static long readTimestampMillis(MemorySegment s, long off) {
        return s.get(ValueLayout.JAVA_LONG_UNALIGNED, off);
    }

    private static int truncateUtf8(byte[] bytes, int maxLen) {
        int len = Math.min(bytes.length, maxLen);
        while (len > 0 && (bytes[len - 1] & 0xC0) == 0x80) {
            len--;
        }
        return len;
    }
}
