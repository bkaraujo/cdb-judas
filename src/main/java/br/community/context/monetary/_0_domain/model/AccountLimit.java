package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record AccountLimit(
        UUID accountId,
        @Nullable BigDecimal creditLimit,
        @Nullable BigDecimal overdraftLimit,
        @Nullable Integer closingDay,
        @Nullable Integer dueDay,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    public AccountLimit(UUID accountId, @Nullable BigDecimal creditLimit, @Nullable BigDecimal overdraftLimit,
                         @Nullable Integer closingDay, @Nullable Integer dueDay) {
        this(accountId, creditLimit, overdraftLimit, closingDay, dueDay, null, null);
    }

    /** True when no limit/day is set — the use case deletes the row instead of persisting this. */
    public boolean isEmpty() {
        return creditLimit == null && overdraftLimit == null && closingDay == null && dueDay == null;
    }
}
