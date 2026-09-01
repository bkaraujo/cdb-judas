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
 * Lançamento. {@code categoryIds} veio do antigo overlay {@code UserTransaction}: mora na tabela
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
        boolean reversal,
        BigDecimal amount,
        LocalDateTime purchasedAt,
        LocalDate installmentDate,
        UUID accountId,
        Status status,
        boolean planned,
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
    /** Accessor de bucketing — data desta parcela (usada em filtros, saldo, extrato). */
    public LocalDate date() { return installmentDate; }

    /** Data real da compra — igual em todas as parcelas do grupo. */
    public LocalDate purchaseDate() { return purchasedAt.toLocalDate(); }

    /** Mesma transação com o vínculo de categoria resolvido (ou limpo, com {@code null}). */
    public Transaction withCategory(@Nullable UUID category) {
        return new Transaction(id, description, reversal, amount, purchasedAt, installmentDate, accountId, status, planned,
                paymentDate, groupId, installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId,
                category, tagIds);
    }

    /** Mesma transação com o vínculo de tags resolvido. */
    public Transaction withTags(List<UUID> tags) {
        return new Transaction(id, description, reversal, amount, purchasedAt, installmentDate, accountId, status, planned,
                paymentDate, groupId, installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId,
                categoryId, tags);
    }

    /**
     * Convenience constructor (14 args): derives signal from nature, takes absolute amount,
     * uses start-of-day for {@code purchasedAt}. {@code installmentDate} is derived as {@code date}
     * — callers that never split the two dates use this form.
     */
    public Transaction(UUID id, String description, BigDecimal amount, LocalDate date,
            UUID accountId, Status status, boolean reversal,
            boolean planned, @Nullable LocalDate paymentDate, @Nullable UUID groupId,
            int installmentNumber, int totalInstallments, @Nullable String notes, @Nullable UUID cardId) {
        this(id, description, reversal, amount.abs(), date.atStartOfDay(), date,
                accountId, status, planned, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, null, null, cardId, null, List.of());
    }

    /**
     * Overload (15 args): separates {@code purchasedAt} (fixed purchase date) from
     * {@code installmentDate} (bucketing date for this installment). Used by
     * {@code WriteUseCases.toEntity} and {@code TransactionWriterAdapter.create}.
     */
    public Transaction(UUID id, String description, BigDecimal amount, LocalDate purchasedAt,
            LocalDate installmentDate,
            UUID accountId, Status status, boolean reversal,
            boolean planned, @Nullable LocalDate paymentDate, @Nullable UUID groupId,
            int installmentNumber, int totalInstallments, @Nullable String notes, @Nullable UUID cardId) {
        this(id, description, reversal, amount.abs(), purchasedAt.atStartOfDay(), installmentDate,
                accountId, status, planned, paymentDate, groupId,
                installmentNumber, totalInstallments, notes, null, null, cardId, null, List.of());
    }

    /**
     * Forma completa sem categoria/tags (17 args) — os vínculos são resolvidos à parte, com
     * {@link #withCategory(UUID)}/{@link #withTags(List)}.
     */
    public Transaction(UUID id, String description, boolean reversal, BigDecimal amount, LocalDateTime purchasedAt,
            LocalDate installmentDate,
            UUID accountId, Status status, boolean planned, @Nullable LocalDate paymentDate,
            @Nullable UUID groupId, int installmentNumber, int totalInstallments, @Nullable String notes,
            @Nullable LocalDateTime createdAt, @Nullable LocalDateTime updatedAt, @Nullable UUID cardId) {
        this(id, description, reversal, amount, purchasedAt, installmentDate, accountId, status, planned, paymentDate,
                groupId, installmentNumber, totalInstallments, notes, createdAt, updatedAt, cardId, null, List.of());
    }

    public int calculateSignal(Nature categoryNature) {
        int base = categoryNature == Nature.INCOME ? 1 : -1;
        return reversal ? -base : base;
    }
}
