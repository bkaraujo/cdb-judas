package br.cdb.feature.f006._0_domain.model;

import br.cdb.feature.f005._0_domain.model.Nature;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lançamento. {@code categoryId} veio do antigo overlay {@code UserTransaction}: mora na tabela
 * {@code F006_TRANSACTION_CATEGORY} (PK {@code (COD_TRANSACTION, COD_PERSON)}), não em
 * {@code F006_TRANSACTION} — {@code TransactionJDBCRepository#save} não o grava junto; a escrita do
 * vínculo é porta própria, {@code TransactionCategoryRepository}. Nas leituras da engine vem
 * {@code null}; quem precisa dele preenche com {@link #withCategory(UUID)} (ver
 * {@code f006._1_application.usecase.ReadUseCases}). {@code tagIds} segue o mesmo molde para
 * {@code F006_TRANSACTION_TAG} (N:N, tabela à parte, porta própria {@code TransactionTagRepository}).
 */
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
        @Nullable UUID cardId,
        @Nullable UUID categoryId,
        List<UUID> tagIds
) {
    /** Convenience accessor returning the purchase date part. */
    public LocalDate date() { return purchasedAt.toLocalDate(); }

    /** Mesma transação com o vínculo de categoria resolvido (ou limpo, com {@code null}). */
    public Transaction withCategory(@Nullable UUID category) {
        return new Transaction(id, description, signal, amount, purchasedAt, accountId, status, costCenterId,
                paymentDate, groupId, installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId,
                category, tagIds);
    }

    /** Mesma transação com o vínculo de tags resolvido. */
    public Transaction withTags(List<UUID> tags) {
        return new Transaction(id, description, signal, amount, purchasedAt, accountId, status, costCenterId,
                paymentDate, groupId, installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId,
                categoryId, tags);
    }

    /** Derived from signal: positive signal = INCOME, non-positive = EXPENSE. */
    public Nature type() { return signal > 0 ? Nature.INCOME : Nature.EXPENSE; }

    /** Convenience constructor: derives signal from type, takes absolute amount, uses start-of-day. */
    public Transaction(UUID id, String description, BigDecimal amount, LocalDate date,
            UUID accountId, Status status, Nature type,
            UUID costCenterId, @Nullable LocalDate paymentDate, @Nullable UUID groupId,
            int installmentNumber, int totalInstallments, @Nullable String notes, @Nullable UUID cardId) {
        this(id, description, type == Nature.INCOME ? 1 : -1, amount.abs(), date.atStartOfDay(),
                accountId, status, costCenterId, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, null, null, cardId, null, List.of());
    }

    /** Forma completa sem categoria/tags — os vínculos são resolvidos à parte, com {@link #withCategory(UUID)}/{@link #withTags(List)}. */
    public Transaction(UUID id, String description, int signal, BigDecimal amount, LocalDateTime purchasedAt,
            UUID accountId, Status status, UUID costCenterId, @Nullable LocalDate paymentDate,
            @Nullable UUID groupId, int installmentNumber, int totalInstallments, @Nullable String notes,
            @Nullable LocalDateTime createdAt, @Nullable LocalDateTime updatedAt, @Nullable UUID cardId) {
        this(id, description, signal, amount, purchasedAt, accountId, status, costCenterId, paymentDate,
                groupId, installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId, null, List.of());
    }
}
