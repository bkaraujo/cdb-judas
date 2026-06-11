package br.community.feature.user.accounts.statement.importer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Preview payload for a bank statement. {@code documentType} is always {@code "BANK_STATEMENT"} so the
 * client can branch from the shared preview endpoint. {@code amount} is signed; {@code type} is
 * {@code "income"}/{@code "expense"}; {@code state} is {@code "NEW"}/{@code "DUPLICATE"}/
 * {@code "RECONCILE"}; {@code reconcileDescription} is the matched manual transaction (RECONCILE only).
 * {@code candidateAccounts} are the destination accounts; {@code selectedAccountId} is the one the
 * states were computed against (null until the user picks one when several exist).
 */
@NullMarked
public record BankStatementPreviewResponse(
        String documentType,
        String issuer,
        List<AccountOption> candidateAccounts,
        @Nullable UUID selectedAccountId,
        List<Row> rows) {

    @NullMarked
    public record Row(
            String date,
            String description,
            BigDecimal amount,
            String type,
            String state,
            @Nullable UUID categoryId,
            @Nullable String reconcileDescription
    ) {}

    @NullMarked
    public record AccountOption(UUID id, String name) {}
}
