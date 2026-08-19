package br.cdb.feature.f006;

import br.cdb.core.View;
import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Cliente da API pública de {@code f006} */
@NullMarked
public interface F006Api {

    /**
     * Projeção mínima de uma transação para quem lê de fora da fatia. {@code amount} vem assinado
     * (negativo para despesa), {@code status}/{@code type} desserializam como enum (ver
     * {@code JsonStorageConfig.transactionEnumModule}), exatamente como o endpoint responde.
     */
    @NullMarked
    record TransactionView(
            UUID id,
            UUID accountId,
            String description,
            BigDecimal amount,
            LocalDate date,
            Transaction.Status status,
            Transaction.Type type,
            @Nullable UUID groupId
    ) implements View {}

    /** Espelha {@code TransactionResponse} — devolvida por criação/atualização/patch de status. */
    @NullMarked
    record TransactionDto(
            UUID id,
            String description,
            BigDecimal amount,
            LocalDate date,
            @Nullable UUID categoryId,
            UUID accountId,
            Transaction.Status status,
            Transaction.Type type,
            UUID costCenterId,
            @Nullable LocalDate paymentDate,
            @Nullable UUID groupId,
            @Nullable Integer installmentNumber,
            @Nullable Integer totalInstallments,
            @Nullable String notes,
            @Nullable UUID cardId,
            List<UUID> tagIds
    ) implements View {}

    /** Corpo de {@link #createTransaction}/{@link #updateTransaction} — espelha {@code TransactionRequest}. */
    @NullMarked
    record TransactionBody(
            String description,
            BigDecimal amount,
            LocalDate date,
            UUID categoryId,
            @Nullable UUID accountId,
            UUID costCenterId,
            Transaction.Status status,
            Transaction.Type type,
            @Nullable Integer installments,
            @Nullable String editMode,
            @Nullable String deleteMode,
            @Nullable String notes,
            @Nullable UUID cardId,
            @Nullable List<UUID> tagIds
    ) {}

    // ── Leitura ────────────────────────────────────────────────────

    /** Todo o histórico da pessoa, sem filtro. */
    List<TransactionView> transactions();

    /**
     * Transações da pessoa, filtradas no servidor — parâmetro nulo não filtra. Evita trazer o
     * histórico inteiro para descartar a maior parte no consumidor.
     */
    List<TransactionView> transactions(@Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo);

    /** Transações de uma conta específica, mesmos filtros de {@link #transactions(String, LocalDate, LocalDate)}. */
    List<TransactionView> transactionsByAccount(UUID accountId, @Nullable String status, @Nullable LocalDate dateFrom, @Nullable LocalDate dateTo);

    /** IDs das transações da pessoa vinculadas a qualquer categoria de {@code categoryIds}. */
    List<UUID> transactionIdsByCategories(Collection<UUID> categoryIds);

    // ── Escrita ────────────────────────────────────────────────────

    TransactionDto createTransaction(UUID accountId, TransactionBody body);

    TransactionDto updateTransaction(UUID accountId, UUID transactionId, TransactionBody body);

    TransactionDto patchStatus(UUID accountId, UUID transactionId, Transaction.Status status, LocalDate paymentDate);

    void deleteTransaction(UUID accountId, UUID transactionId, @Nullable String mode);

    TransactionDto transfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount);

}
