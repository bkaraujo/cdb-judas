package br.cdb.feature.f004._1_application.cache;

import br.commons.platform.NativeCache;
import br.cdb.feature.f004._0_domain.model.Tag;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public final class TagLayout {
    public static final String PREFIX = "TAG:";

    private static final long OFF_ID_MSB = 0;
    private static final long OFF_ID_LSB = 8;
    private static final long OFF_PERSON_ID_MSB = 16;
    private static final long OFF_PERSON_ID_LSB = 24;
    private static final long OFF_NAME_LEN = 32;
    private static final long OFF_NAME_DATA = 36;
    private static final int NAME_CAPACITY = 1024;
    private static final long OFF_COLOR_LEN = OFF_NAME_DATA + NAME_CAPACITY;
    private static final long OFF_COLOR_DATA = OFF_COLOR_LEN + 4;
    private static final int COLOR_CAPACITY = 84;
    private static final long OFF_CREATED_AT = OFF_COLOR_DATA + COLOR_CAPACITY;

    static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(1,
            MemoryLayout.structLayout(
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id LSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // personId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // personId LSB
                    ValueLayout.JAVA_INT_UNALIGNED,   // name length
                    MemoryLayout.sequenceLayout(NAME_CAPACITY, ValueLayout.JAVA_BYTE),  // name data
                    ValueLayout.JAVA_INT_UNALIGNED,   // color length
                    MemoryLayout.sequenceLayout(COLOR_CAPACITY, ValueLayout.JAVA_BYTE),  // color data
                    ValueLayout.JAVA_LONG_UNALIGNED   // createdAt millis
            ));

    public static final long SIZE = 16 + 16 + 4 + NAME_CAPACITY + 4 + COLOR_CAPACITY + 8;

    public static void write(MemorySegment target, Tag model) {
        NativeCache.writeUuid(target, OFF_ID_MSB, model.id());
        NativeCache.writeUuid(target, OFF_PERSON_ID_MSB, model.personId());
        NativeCache.writeString(target, OFF_NAME_LEN, 4 + NAME_CAPACITY, model.name());
        NativeCache.writeString(target, OFF_COLOR_LEN, 4 + COLOR_CAPACITY, model.color());
        NativeCache.writeTimestamp(target, OFF_CREATED_AT, model.createdAt());
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
        public @Nullable String color() { return NativeCache.readString(segment, OFF_COLOR_LEN); }

        public long createdAtMillis() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_CREATED_AT); }
        public @Nullable LocalDateTime createdAt() {
            val millis = createdAtMillis();
            if (millis == NativeCache.NULL_LONG) return null;
            return LocalDateTime.ofEpochSecond(millis / 1000, (int) ((millis % 1000) * 1_000_000), java.time.ZoneOffset.UTC);
        }
    }
}
