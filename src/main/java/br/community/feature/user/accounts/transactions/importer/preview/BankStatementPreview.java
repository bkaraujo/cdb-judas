package br.community.feature.user.accounts.transactions.importer.preview;

import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.feature.user.accounts.statement.Issuer;
import br.community.feature.user.accounts.transactions.importer.RowState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Bank-statement import preview: the detected issuer, the destination accounts offered for manual
 * pick (non-credit-card accounts), the account currently selected (null until the user picks one, or
 * the lone candidate) and the parsed rows. Row states (duplicate/reconcile) are meaningful only once
 * an account is selected; with no account they are all {@link RowState#NEW}.
 */
@NullMarked
public record BankStatementPreview(
        Issuer issuer,
        List<MonetaryAccount> candidateAccounts,
        @Nullable UUID selectedAccountId,
        List<BankStatementPreviewRow> rows) {}
