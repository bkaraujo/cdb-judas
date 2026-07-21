package br.cdb.feature.user;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.dashboard.DashboardService;
import br.cdb.feature.f002._1_application.AccountStreamPublisher;
import br.cdb.feature.finance.accounts.transactions.importer.ImportError;
import br.cdb.feature.finance.accounts.transactions.importer.ImportResult;
import br.cdb.feature.finance.accounts.transactions.importer.StatementImportService;
import br.cdb.feature.finance.accounts.transactions.importer.confirm.BankStatementConfirmCommand;
import br.cdb.feature.finance.accounts.transactions.importer.confirm.InvoiceConfirmCommand;
import br.cdb.feature.finance.accounts.transactions.importer.preview.ImportPreviewOutcome;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O que restou do antigo god-object da fatia {@code feature.user}, após a reestruturação fNNN
 * (.claude/refactor.md) extrair accounts/cards/balance (f002), transactions/transfer (f005),
 * tags (f008) e categories (f007) para seus próprios use cases: só importação de extrato
 * (candidata a f006) e dashboard (candidata a f009) continuam aqui.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserUseCase {

    private final AccountUseCase ucAccount = MonetaryUseCases.ucAccount();
    private final CreditCardUseCase ucCreditCard = MonetaryUseCases.ucCreditCard();

    private final UserGuards guards;
    private final DashboardService dashboardService;
    private final StatementImportService statementImportService;
    private final AccountStreamPublisher accountStreamPublisher;

    /** Preview + nomes de conta por personId (rotulam as opções de cartão na resposta). */
    @NullMarked
    public record ImportPreviewView(ImportPreviewOutcome outcome, Map<UUID, String> accountNamesById) {}

    // ── Statement import ───────────────────────────────────────────

    public Result<ImportPreviewView, ImportError> importPreview(byte[] fileBytes, @Nullable String password, @Nullable UUID accountId) {
        if (accountId != null && guards.ownsAccount(accountId).isFailure()) {
            return new Result.Failure<>(new ImportError.AccountNotFound());
        }

        return statementImportService.preview(fileBytes, password, accountId)
                .map(outcome -> new ImportPreviewView(outcome, accountNamesById()));
    }

    public Result<ImportResult, BusinessError> confirmInvoiceImport(InvoiceConfirmCommand cmd) {
        for (val row : cmd.rows()) {
            if (guards.ownsCard(row.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
        }
        return statementImportService.confirm(cmd)
                .ifSuccess(ignored -> affectedAccountIds(cmd.rows()).forEach(accountStreamPublisher::upsert));
    }

    public Result<ImportResult, BusinessError> confirmStatementImport(BankStatementConfirmCommand cmd) {
        return guards.ownsAccount(cmd.accountId()).flatMap(ignored -> statementImportService.confirmStatement(cmd))
                .ifSuccess(ignored -> accountStreamPublisher.upsert(cmd.accountId()));
    }

    /** Nome da conta a que cada cartão pertence, para rotular as opções de cartão do preview. */
    private Map<UUID, String> accountNamesById() {
        return ucAccount.listAccounts().getOrElse(List.of()).stream()
                .collect(Collectors.toMap(Account::id, Account::name));
    }

    /** Contas distintas donas dos cartões das linhas confirmadas. */
    private List<UUID> affectedAccountIds(List<InvoiceConfirmCommand.Row> rows) {
        val accountByCard = ucCreditCard.list().getOrElse(List.of()).stream()
                .collect(Collectors.toMap(CreditCard::id, CreditCard::accountId));
        return rows.stream()
                .map(row -> accountByCard.get(row.cardId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // ── Dashboard ──────────────────────────────────────────────────

    public Result<DashboardService.MonthlyResult, BusinessError> monthlyResult(int month, int year) {
        return dashboardService.getMonthlyResult(month, year);
    }
}
