package br.cdb.feature.f003._1_application.cache;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.commons.platform.NativeCache;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public final class CreditCardLayout {
    public static final String PREFIX = "CARD:";

    private static final long OFF_ID_MSB = 0;
    private static final long OFF_ID_LSB = 8;
    private static final long OFF_LAST4_LEN = 16;
    private static final long OFF_LAST4_DATA = 20;
    private static final int LAST4_CAPACITY = 20;
    private static final long OFF_ACCOUNT_ID_MSB = OFF_LAST4_DATA + LAST4_CAPACITY;
    private static final long OFF_ACCOUNT_ID_LSB = OFF_ACCOUNT_ID_MSB + 8;
    private static final long OFF_ACTIVE = OFF_ACCOUNT_ID_LSB + 8;
    private static final long OFF_CREATED_AT = OFF_ACTIVE + 1;
    private static final long OFF_UPDATED_AT = OFF_CREATED_AT + 8;

    static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(1,
            MemoryLayout.structLayout(
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id LSB
                    ValueLayout.JAVA_INT_UNALIGNED,   // last4 length
                    MemoryLayout.sequenceLayout(LAST4_CAPACITY, ValueLayout.JAVA_BYTE),  // last4 data
                    ValueLayout.JAVA_LONG_UNALIGNED,  // accountId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // accountId LSB
                    ValueLayout.JAVA_BYTE,            // active
                    ValueLayout.JAVA_LONG_UNALIGNED,  // createdAt millis
                    ValueLayout.JAVA_LONG_UNALIGNED   // updatedAt millis
            ));

    public static final long SIZE = 16 + 4 + LAST4_CAPACITY + 16 + 1 + 8 + 8;

    public static void write(MemorySegment target, CreditCard model) {
        NativeCache.writeUuid(target, OFF_ID_MSB, model.id());
        NativeCache.writeString(target, OFF_LAST4_LEN, 4 + LAST4_CAPACITY, model.last4());
        NativeCache.writeUuid(target, OFF_ACCOUNT_ID_MSB, model.accountId());
        NativeCache.writeBoolean(target, OFF_ACTIVE, model.active());
        NativeCache.writeTimestamp(target, OFF_CREATED_AT, model.createdAt());
        NativeCache.writeTimestamp(target, OFF_UPDATED_AT, model.updatedAt());
    }

    @NullMarked
    public static final class View {
        private MemorySegment segment = MemorySegment.NULL;

        public View bind(MemorySegment s) {
            this.segment = s;
            return this;
        }

        public long idMsb() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_ID_MSB); }
        public long idLsb() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_ID_LSB); }
        public UUID id() { return NativeCache.readUuid(segment, OFF_ID_MSB); }

        public @Nullable String last4() { return NativeCache.readString(segment, OFF_LAST4_LEN); }

        public UUID accountId() { return NativeCache.readUuid(segment, OFF_ACCOUNT_ID_MSB); }

        public boolean active() { return NativeCache.readBoolean(segment, OFF_ACTIVE); }

        public long createdAtMillis() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_CREATED_AT); }
        public @Nullable LocalDateTime createdAt() {
            val millis = createdAtMillis();
            if (millis == NativeCache.NULL_LONG) return null;
            return LocalDateTime.ofEpochSecond(millis / 1000, (int) ((millis % 1000) * 1_000_000), java.time.ZoneOffset.UTC);
        }

        public long updatedAtMillis() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_UPDATED_AT); }
        public @Nullable LocalDateTime updatedAt() {
            val millis = updatedAtMillis();
            if (millis == NativeCache.NULL_LONG) return null;
            return LocalDateTime.ofEpochSecond(millis / 1000, (int) ((millis % 1000) * 1_000_000), java.time.ZoneOffset.UTC);
        }
    }
}
