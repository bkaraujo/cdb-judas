package br.cdb.feature.f007._0_domain.model;

import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f007._1_application.TransactionWriter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha de importação pronta para persistir, passada a {@link TransactionWriter#create}. Espelha
 * os campos do construtor de conveniência de {@code Transaction} usados por
 * {@code InvoiceImportProcessor}/{@code StatementImportProcessor} — sem {@code id} (gerado pelo
 * adapter), sem {@code paymentDate}/{@code notes} (sempre nulos na importação).
 */
@NullMarked
public record ImportedTransaction(
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID accountId,
        Transaction.Status status,
        Nature type,
        UUID costCenterId,
        @Nullable UUID groupId,
        @Nullable Integer installmentNumber,
        @Nullable Integer totalInstallments,
        @Nullable UUID cardId
) {}
