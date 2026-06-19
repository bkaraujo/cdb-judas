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
        UUID categoryId,
        UUID accountId,
        Status status,
        Type type,
        UUID costCenterId,
        @Nullable LocalDate paymentDate,
        @Nullable UUID groupId,
        int installmentNumber,
        int totalInstallments,
        @Nullable String notes,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
    /** Convenience accessor returning the purchase date part, for code not yet migrated to LocalDateTime. */
    public LocalDate date() { return purchasedAt.toLocalDate(); }
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

    public Transaction(UUID id, String description, BigDecimal amount, LocalDate date,
            UUID categoryId, UUID accountId, Status status, Type type,
            UUID costCenterId, @Nullable LocalDate paymentDate, @Nullable UUID groupId,
            int installmentNumber, int totalInstallments, @Nullable String notes) {
        this(id, description, type == Type.INCOME ? 1 : -1, amount.abs(), date.atStartOfDay(),
                categoryId, accountId, status, type, costCenterId, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, null, null);
    }
}
