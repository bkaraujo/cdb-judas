package br.cdb.feature.f007._1_application.preview;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f007._0_domain.RowState;
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
        String issuer,
        List<Account> candidateAccounts,
        @Nullable UUID selectedAccountId,
        List<BankStatementPreviewRow> rows) {}
