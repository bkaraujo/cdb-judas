package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record Transaction(
        UUID id,
        String description,
        int signal,
        BigDecimal amount,
        LocalDateTime purchasedAt,
        UUID accountId,
        Status status,
        UUID costCenterId,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        int installmentNumber,
        int totalInstallments,
        @Nullable String notes,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt,
        @Nullable UUID cardId
) {
    /** Convenience accessor returning the purchase date part. */
    public LocalDate date() { return purchasedAt.toLocalDate(); }

    /** Derived from signal: positive signal = INCOME, non-positive = EXPENSE. */
    public Type type() { return signal > 0 ? Type.INCOME : Type.EXPENSE; }

    /**
     * Natureza de um {@link Transaction}
     * <ul>
     *     <li>{@link #EXPENSE} (saída, sinal negativo)</li>
     *     <li>{@link #INCOME} (entrada, sinal positivo)</li>
     * </ul>
     */
    public enum Type {
        EXPENSE, INCOME
    }

    /**
     * Situação de um {@link Transaction}
     * <ul>
     *     <li>{@link #SCHEDULED} Planejado mas ainda não executado</li>
     *     <li>{@link #CONFIRMED} Executado</li>
     *     <li>{@link #PENDING} Atrasádo/Pendente de Pagamento</li>
     * </ul>
     */
    public enum Status {
        SCHEDULED, CONFIRMED, PENDING
    }

    /** Convenience constructor: derives signal from type, takes absolute amount, uses start-of-day. */
    public Transaction(UUID id, String description, BigDecimal amount, LocalDate date,
            UUID accountId, Status status, Type type,
            UUID costCenterId, @Nullable LocalDate paymentDate, @Nullable UUID groupId,
            int installmentNumber, int totalInstallments, @Nullable String notes, @Nullable UUID cardId) {
        this(id, description, type == Type.INCOME ? 1 : -1, amount.abs(), date.atStartOfDay(),
                accountId, status, costCenterId, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, null, null, cardId);
    }
}
