package br.cdb.feature.f006._2_infrastructure.web.response;

import br.cdb.feature.f006._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Preview payload for a bank statement. {@code documentType} is always {@code "BANK_STATEMENT"} so the
 * client can branch from the shared preview endpoint. {@code amount} is signed; {@code type} is
 * {@code INCOME}/{@code EXPENSE}; {@code state} is {@code "NEW"}/{@code "DUPLICATE"}/
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
            Transaction.Type type,
            String state,
            @Nullable UUID categoryId,
            @Nullable UUID costCenterId,
            @Nullable String reconcileDescription
    ) {}

    @NullMarked
    public record AccountOption(UUID id, String name) {}
}
