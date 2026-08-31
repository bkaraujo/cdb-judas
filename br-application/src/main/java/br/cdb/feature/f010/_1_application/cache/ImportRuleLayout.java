package br.cdb.feature.f010._1_application.cache;

import br.commons.platform.NativeCache;
import br.cdb.feature.f010._0_domain.model.ImportRule;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NullMarked
public final class ImportRuleLayout {
    public static final String PREFIX = "RULE:";

    private static final long OFF_ID_MSB = 0;
    private static final long OFF_ID_LSB = 8;
    private static final long OFF_PERSON_ID_MSB = 16;
    private static final long OFF_PERSON_ID_LSB = 24;
    private static final long OFF_NAME_LEN = 32;
    private static final long OFF_NAME_DATA = 36;
    private static final int NAME_CAPACITY = 1024;
    private static final long OFF_ACCOUNT_ID_MSB = OFF_NAME_DATA + NAME_CAPACITY;
    private static final long OFF_ACCOUNT_ID_LSB = OFF_ACCOUNT_ID_MSB + 8;
    private static final long OFF_CATEGORY_ID_MSB = OFF_ACCOUNT_ID_LSB + 8;
    private static final long OFF_CATEGORY_ID_LSB = OFF_CATEGORY_ID_MSB + 8;
    private static final long OFF_PLANNED = OFF_CATEGORY_ID_LSB + 8;
    private static final long OFF_CREATED_AT = OFF_PLANNED + 1;
    private static final long OFF_TRIGGER_COUNT = OFF_CREATED_AT + 8;
    private static final long OFF_TRIGGERS_BLOB = OFF_TRIGGER_COUNT + 4;

    public static final long HEADER_SIZE = OFF_TRIGGERS_BLOB;
    public static final long MAX_TRIGGERS_BLOB = 64 * 1024 - HEADER_SIZE;

    static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(1,
            MemoryLayout.structLayout(
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id LSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // personId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // personId LSB
                    ValueLayout.JAVA_INT_UNALIGNED,   // name length
                    MemoryLayout.sequenceLayout(NAME_CAPACITY, ValueLayout.JAVA_BYTE),  // name data
                    ValueLayout.JAVA_LONG_UNALIGNED,  // accountId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // accountId LSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // categoryId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // categoryId LSB
                    ValueLayout.JAVA_BYTE,            // planned (-1=null, 0=false, 1=true)
                    ValueLayout.JAVA_LONG_UNALIGNED,  // createdAt millis
                    ValueLayout.JAVA_INT_UNALIGNED    // trigger count
            ));

    public static long write(MemorySegment target, ImportRule model) {
        NativeCache.writeUuid(target, OFF_ID_MSB, model.id());
        NativeCache.writeUuid(target, OFF_PERSON_ID_MSB, model.personId());
        NativeCache.writeString(target, OFF_NAME_LEN, 4 + NAME_CAPACITY, model.name());
        NativeCache.writeUuid(target, OFF_ACCOUNT_ID_MSB, model.accountId());
        NativeCache.writeUuid(target, OFF_CATEGORY_ID_MSB, model.categoryId());

        if (model.planned() == null) {
            target.set(ValueLayout.JAVA_BYTE, OFF_PLANNED, (byte) -1);
        } else {
            target.set(ValueLayout.JAVA_BYTE, OFF_PLANNED, model.planned() ? (byte) 1 : (byte) 0);
        }

        NativeCache.writeTimestamp(target, OFF_CREATED_AT, model.createdAt());

        val triggers = model.triggers();
        val triggerCount = Math.min(triggers.size(), 64);
        target.set(ValueLayout.JAVA_INT_UNALIGNED, OFF_TRIGGER_COUNT, triggerCount);

        long blobOffset = OFF_TRIGGERS_BLOB;
        for (int i = 0; i < triggerCount; i++) {
            val trigger = triggers.get(i);
            val bytes = trigger.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (blobOffset + 4 + bytes.length > HEADER_SIZE + MAX_TRIGGERS_BLOB) break;

            target.set(ValueLayout.JAVA_INT_UNALIGNED, blobOffset, bytes.length);
            blobOffset += 4;
            MemorySegment.copy(MemorySegment.ofArray(bytes), 0, target, blobOffset, bytes.length);
            blobOffset += bytes.length;
        }

        return HEADER_SIZE + (blobOffset - OFF_TRIGGERS_BLOB);
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

        public long personIdMsb() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_PERSON_ID_MSB); }
        public long personIdLsb() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_PERSON_ID_LSB); }
        public UUID personId() { return NativeCache.readUuid(segment, OFF_PERSON_ID_MSB); }

        public @Nullable String name() { return NativeCache.readString(segment, OFF_NAME_LEN); }

        public @Nullable UUID accountId() { return NativeCache.readUuid(segment, OFF_ACCOUNT_ID_MSB); }
        public @Nullable UUID categoryId() { return NativeCache.readUuid(segment, OFF_CATEGORY_ID_MSB); }

        public @Nullable Boolean planned() {
            val b = segment.get(ValueLayout.JAVA_BYTE, OFF_PLANNED);
            return b == -1 ? null : b != 0;
        }

        public long createdAtMillis() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_CREATED_AT); }
        public @Nullable LocalDateTime createdAt() {
            val millis = createdAtMillis();
            if (millis == NativeCache.NULL_LONG) return null;
            return LocalDateTime.ofEpochSecond(millis / 1000, (int) ((millis % 1000) * 1_000_000), java.time.ZoneOffset.UTC);
        }

        public int triggerCount() {
            return segment.get(ValueLayout.JAVA_INT_UNALIGNED, OFF_TRIGGER_COUNT);
        }

        public @Nullable String trigger(int index) {
            val count = triggerCount();
            if (index < 0 || index >= count) return null;

            long blobOffset = OFF_TRIGGERS_BLOB;
            for (int i = 0; i < index; i++) {
                val len = segment.get(ValueLayout.JAVA_INT_UNALIGNED, blobOffset);
                blobOffset += 4 + len;
            }

            val len = segment.get(ValueLayout.JAVA_INT_UNALIGNED, blobOffset);
            val bytes = new byte[len];
            MemorySegment.copy(segment, blobOffset + 4, MemorySegment.ofArray(bytes), 0, len);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
