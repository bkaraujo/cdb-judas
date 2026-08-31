package br.cdb.feature.f002._1_application.cache;

import br.commons.platform.NativeCache;
import br.cdb.feature.f002._0_domain.model.Account;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public final class AccountLayout {
    public static final String PREFIX = "ACCT:";

    private static final long OFF_ID_MSB = 0;
    private static final long OFF_ID_LSB = 8;
    private static final long OFF_NAME_LEN = 16;
    private static final long OFF_NAME_DATA = 20;
    private static final int NAME_CAPACITY = 324;
    private static final long OFF_TYPE = OFF_NAME_DATA + NAME_CAPACITY;
    private static final long OFF_ACTIVE = OFF_TYPE + 1;
    private static final long OFF_PERSON_ID_LEN = OFF_ACTIVE + 1;
    private static final long OFF_PERSON_ID_DATA = OFF_PERSON_ID_LEN + 4;
    private static final int PERSON_ID_CAPACITY = 40;
    private static final long OFF_COLOR_LEN = OFF_PERSON_ID_DATA + PERSON_ID_CAPACITY;
    private static final long OFF_COLOR_DATA = OFF_COLOR_LEN + 4;
    private static final int COLOR_CAPACITY = 84;
    private static final long OFF_CREDIT_LIMIT = OFF_COLOR_DATA + COLOR_CAPACITY;
    private static final long OFF_OVERDRAFT_LIMIT = OFF_CREDIT_LIMIT + 8;
    private static final long OFF_CLOSING_DAY = OFF_OVERDRAFT_LIMIT + 8;
    private static final long OFF_DUE_DAY = OFF_CLOSING_DAY + 4;
    private static final long OFF_CREATED_AT = OFF_DUE_DAY + 4;
    private static final long OFF_UPDATED_AT = OFF_CREATED_AT + 8;

    public static final long SIZE = OFF_UPDATED_AT + 8;

    public static void write(MemorySegment target, Account model) {
        NativeCache.writeUuid(target, OFF_ID_MSB, model.id());
        NativeCache.writeString(target, OFF_NAME_LEN, 4 + NAME_CAPACITY, model.name());
        target.set(ValueLayout.JAVA_BYTE, OFF_TYPE, (byte) model.type().ordinal());
        NativeCache.writeBoolean(target, OFF_ACTIVE, model.active());
        NativeCache.writeString(target, OFF_PERSON_ID_LEN, 4 + PERSON_ID_CAPACITY, model.personId());
        NativeCache.writeString(target, OFF_COLOR_LEN, 4 + COLOR_CAPACITY, model.color());
        NativeCache.writeMoney(target, OFF_CREDIT_LIMIT, model.creditLimit());
        NativeCache.writeMoney(target, OFF_OVERDRAFT_LIMIT, model.overdraftLimit());
        target.set(ValueLayout.JAVA_INT_UNALIGNED, OFF_CLOSING_DAY, model.closingDay() == null ? NativeCache.NULL_INT : model.closingDay());
        target.set(ValueLayout.JAVA_INT_UNALIGNED, OFF_DUE_DAY, model.dueDay() == null ? NativeCache.NULL_INT : model.dueDay());
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

        public @Nullable String name() { return NativeCache.readString(segment, OFF_NAME_LEN); }

        public Account.Type type() {
            val ord = segment.get(ValueLayout.JAVA_BYTE, OFF_TYPE);
            return Account.Type.values()[ord];
        }

        public boolean active() { return NativeCache.readBoolean(segment, OFF_ACTIVE); }

        public @Nullable String personId() { return NativeCache.readString(segment, OFF_PERSON_ID_LEN); }

        public @Nullable String color() { return NativeCache.readString(segment, OFF_COLOR_LEN); }

        public @Nullable BigDecimal creditLimit() {
            val cents = NativeCache.readMoneyCents(segment, OFF_CREDIT_LIMIT);
            if (cents == NativeCache.NULL_LONG) return null;
            return BigDecimal.valueOf(cents, 2);
        }

        public @Nullable BigDecimal overdraftLimit() {
            val cents = NativeCache.readMoneyCents(segment, OFF_OVERDRAFT_LIMIT);
            if (cents == NativeCache.NULL_LONG) return null;
            return BigDecimal.valueOf(cents, 2);
        }

        public @Nullable Integer closingDay() {
            val v = segment.get(ValueLayout.JAVA_INT_UNALIGNED, OFF_CLOSING_DAY);
            return v == NativeCache.NULL_INT ? null : v;
        }

        public @Nullable Integer dueDay() {
            val v = segment.get(ValueLayout.JAVA_INT_UNALIGNED, OFF_DUE_DAY);
            return v == NativeCache.NULL_INT ? null : v;
        }

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
