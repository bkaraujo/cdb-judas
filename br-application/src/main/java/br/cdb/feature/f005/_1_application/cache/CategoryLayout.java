package br.cdb.feature.f005._1_application.cache;

import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.model.Nature;
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
public final class CategoryLayout {
    public static final String PREFIX = "CAT:";

    private static final long OFF_ID_MSB = 0;
    private static final long OFF_ID_LSB = 8;
    private static final long OFF_PERSON_ID_MSB = 16;
    private static final long OFF_PERSON_ID_LSB = 24;
    private static final long OFF_NATURE = 32;
    private static final long OFF_NAME_LEN = 33;
    private static final long OFF_NAME_DATA = 37;
    private static final int NAME_CAPACITY = 324;
    private static final long OFF_PARENT_ID_MSB = OFF_NAME_DATA + NAME_CAPACITY;
    private static final long OFF_PARENT_ID_LSB = OFF_PARENT_ID_MSB + 8;
    private static final long OFF_IS_SYSTEM = OFF_PARENT_ID_LSB + 8;
    private static final long OFF_ACTIVE = OFF_IS_SYSTEM + 1;
    private static final long OFF_CREATED_AT = OFF_ACTIVE + 1;
    private static final long OFF_UPDATED_AT = OFF_CREATED_AT + 8;

    static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(1,
            MemoryLayout.structLayout(
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // id LSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // personId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // personId LSB
                    ValueLayout.JAVA_BYTE,            // nature ordinal
                    ValueLayout.JAVA_INT_UNALIGNED,   // name length
                    MemoryLayout.sequenceLayout(NAME_CAPACITY, ValueLayout.JAVA_BYTE),  // name data
                    ValueLayout.JAVA_LONG_UNALIGNED,  // parentId MSB
                    ValueLayout.JAVA_LONG_UNALIGNED,  // parentId LSB
                    ValueLayout.JAVA_BYTE,            // isSystem
                    ValueLayout.JAVA_BYTE,            // active
                    ValueLayout.JAVA_LONG_UNALIGNED,  // createdAt millis
                    ValueLayout.JAVA_LONG_UNALIGNED   // updatedAt millis
            ));

    public static final long SIZE = 16 + 16 + 1 + 4 + NAME_CAPACITY + 16 + 1 + 1 + 8 + 8;

    public static void write(MemorySegment target, Category model) {
        NativeCache.writeUuid(target, OFF_ID_MSB, model.id());
        NativeCache.writeUuid(target, OFF_PERSON_ID_MSB, model.personId());
        target.set(ValueLayout.JAVA_BYTE, OFF_NATURE, (byte) model.nature().ordinal());
        NativeCache.writeString(target, OFF_NAME_LEN, 4 + NAME_CAPACITY, model.name());
        NativeCache.writeUuid(target, OFF_PARENT_ID_MSB, model.parentId());
        NativeCache.writeBoolean(target, OFF_IS_SYSTEM, model.isSystem());
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

        public long personIdMsb() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_PERSON_ID_MSB); }
        public long personIdLsb() { return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, OFF_PERSON_ID_LSB); }
        public UUID personId() { return NativeCache.readUuid(segment, OFF_PERSON_ID_MSB); }

        public Nature nature() {
            val ord = segment.get(ValueLayout.JAVA_BYTE, OFF_NATURE);
            return Nature.values()[ord];
        }

        public @Nullable String name() { return NativeCache.readString(segment, OFF_NAME_LEN); }

        public @Nullable UUID parentId() { return NativeCache.readUuid(segment, OFF_PARENT_ID_MSB); }

        public boolean isSystem() { return NativeCache.readBoolean(segment, OFF_IS_SYSTEM); }
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
