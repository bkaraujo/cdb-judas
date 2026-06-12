package br.community.feature.user.accounts.transactions.importer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Unified confirm-import payload for both credit-card invoices and bank statements. The
 * {@code type} discriminator (which the preview already returns) tells the server which confirm
 * path to follow. Fields specific to one path are {@code @Nullable} and validated by the resource
 * layer according to the declared type.
 */
@NullMarked
public record StatementConfirmRequest(
        @NotNull StatementType type,
        @Nullable UUID accountId,
        @NotNull @Valid List<Row> rows
) {
    public enum StatementType { CREDIT_CARD_INVOICE, BANK_STATEMENT }

    @NullMarked
    public record Row(
            @NotBlank String description,
            @NotNull BigDecimal amount,
            @NotNull LocalDate date,
            @Nullable LocalDate originalDate,
            @Nullable Integer installmentNumber,
            @Nullable Integer installmentTotal,
            @Nullable String transactionType,
            @NotNull UUID categoryId,
            @Nullable UUID cardId
    ) {}
}
