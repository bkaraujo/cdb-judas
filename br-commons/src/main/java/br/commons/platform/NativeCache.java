package br.commons.platform;

import br.commons.Logger;
import br.commons.Result;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

/**
 * Arena off-heap indexada por chave. Toda operação que pode falhar — cache já fechado, chave
 * ausente — devolve {@link Result}: a API pública não lança nem entrega {@code null} para o
 * chamador decidir o que fazer. A exceção é {@link #segment}, o acesso de caminho quente, onde um
 * {@code Result} por leitura seria uma alocação por linha lida. Os codecs estáticos também ficam
 * fora dessa regra de propósito: são primitivas de serialização puras, com perda definida
 * (trunca/satura + log), consumidas pelos {@code Layout.write} através de um {@code BiConsumer}.
 *
 * <p>Genérico na chave: uma instância guarda um tipo só de registro, então a chave natural é o id
 * do registro ({@code UUID}), não uma String com prefixo de tipo. Filtrar por prefixo aqui dentro
 * era um {@code startsWith} por registro que nunca descartava nada.
 */
@NullMarked
public class NativeCache<K> implements AutoCloseable {
    private static final LocalDateTime EPOCH_UTC = LocalDateTime.of(1970, 1, 1, 0, 0);

    /** Erro devolvido por qualquer operação depois do {@link #close()}. */
    public static final String ERROR_CLOSED = "NativeCache is closed";

    /** Erro de chave ausente. Constante: o miss é rotina, não paga concatenação por chamada. */
    public static final String ERROR_MISS = "NativeCache miss";

    public static final long NULL_LONG = Long.MIN_VALUE;
    public static final int  NULL_INT  = Integer.MIN_VALUE;

    private final Arena arena;
    private final Map<K, MemorySegment> index = new ConcurrentHashMap<>();
    /** Ordem de inserção das chaves — o índice é hash, e listagem sem ordem estável é listagem instável. */
    private final Queue<K> order = new ConcurrentLinkedQueue<>();
    private volatile boolean closed = false;

    public NativeCache() {
        arena = Arena.ofShared();
    }

    /** Aloca (sempre um segmento novo, zerado) e indexa em {@code key}. */
    public Result<MemorySegment, String> put(K key, long size) {
        if (closed) return Result.failure(ERROR_CLOSED);
        val segment = arena.allocate(size);
        MemorySegment.copy(MemorySegment.ofArray(new byte[(int) size]), 0, segment, 0, size);
        // Reescrever uma chave mantém a posição original: só chave nova entra no fim da ordem.
        if (index.put(key, segment) == null) order.add(key);
        return Result.success(segment);
    }

    /** Falha em miss: ausência de chave é resultado normal, mas nunca um segmento utilizável. */
    public Result<MemorySegment, String> get(K key) {
        if (closed) return Result.failure(ERROR_CLOSED);
        val segment = index.get(key);
        if (segment == null) return Result.failure(ERROR_MISS);
        return Result.success(segment);
    }

    /**
     * Acesso de caminho quente: o segmento, ou {@code null} se a chave não está aqui (ou o cache já
     * fechou). Mesma leitura de {@link #get}, sem o {@link Result} — que numa listagem ou num find
     * por linha vira uma alocação por leitura, sem nada a informar além da própria ausência.
     */
    public @Nullable MemorySegment segment(K key) {
        if (closed) return null;
        return index.get(key);
    }

    /**
     * Visita cada segmento indexado, na ordem de inserção. Um {@code Result} para a travessia
     * inteira, não um por registro: este é o caminho de leitura zero-alloc dos flyweights
     * ({@code Layout.View}), onde embrulhar registro a registro anularia o ganho.
     */
    public Result<Void, String> forEach(BiConsumer<K, MemorySegment> visitor) {
        if (closed) return Result.failure(ERROR_CLOSED);
        for (val key : order) {
            val segment = index.get(key);
            if (segment != null) visitor.accept(key, segment);
        }
        return Result.success();
    }

    public Result<Void, String> remove(K key) {
        if (closed) return Result.failure(ERROR_CLOSED);
        if (index.remove(key) != null) order.remove(key);
        return Result.success();
    }

    /** Chaves indexadas, na ordem de inserção. */
    public Result<List<K>, String> keys() {
        if (closed) return Result.failure(ERROR_CLOSED);
        val result = new ArrayList<K>();
        for (val key : order) {
            if (index.containsKey(key)) {
                result.add(key);
            }
        }
        return Result.success(result);
    }

    public Result<Integer, String> size() {
        if (closed) return Result.failure(ERROR_CLOSED);
        return Result.success(index.size());
    }

    /** Quantidade de registros indexados; {@code 0} depois do {@link #close()}. Sem {@link Result}
     *  porque existe para dimensionar a coleção de destino antes de uma travessia. */
    public int count() {
        return closed ? 0 : index.size();
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
        order.clear();
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
                Logger.warn("String truncated from %s to %s bytes", bytes.length, len);
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
                Logger.warn("BigDecimal overflow: %s", v);
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
