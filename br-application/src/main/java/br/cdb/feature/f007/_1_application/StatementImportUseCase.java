package br.cdb.feature.f007._1_application;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f002._1_application.usecase.AccountUseCase;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f007._0_domain.ImportError;
import br.cdb.feature.f007._0_domain.ImportResult;
import br.cdb.feature.f007._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f007._1_application.confirm.StatementConfirmCommand;
import br.cdb.feature.f007._1_application.preview.ImportPreviewOutcome;
import br.commons.MessageBus;
import br.commons.Registry;
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
 * Use case da fatia {@code f007} (importação de extrato/fatura). SSE de conta publica
 * {@link AccountStreamEvents.Refresh} — dispatch é responsabilidade única de {@code f999}.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class StatementImportUseCase {

    private final AccountUseCase ucAccount = Registry.tryGet(AccountUseCase.class);
    private final CreditCardUseCase ucCreditCard = Registry.tryGet(CreditCardUseCase.class);

    private final UserGuards guards;
    private final StatementImportService service;

    /** Preview + nomes de conta por personId (rotulam as opções de cartão na resposta). */
    @NullMarked
    public record ImportPreviewView(ImportPreviewOutcome outcome, Map<UUID, String> accountNamesById) {}

    public Result<ImportPreviewView, ImportError> importPreview(byte[] fileBytes, @Nullable String password, @Nullable UUID accountId) {
        if (accountId != null && guards.ownsAccount(accountId).isFailure()) {
            return new Result.Failure<>(new ImportError.AccountNotFound());
        }

        return service.preview(fileBytes, password, accountId)
                .map(outcome -> new ImportPreviewView(outcome, accountNamesById()));
    }

    public Result<ImportResult, BusinessError> confirmInvoiceImport(UUID personId, InvoiceConfirmCommand cmd) {
        for (val row : cmd.rows()) {
            if (guards.ownsCard(row.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
        }
        return service.confirm(personId, cmd)
                .ifSuccess(ignored -> affectedAccountIds(cmd.rows())
                        .forEach(accountId -> MessageBus.submit(new AccountStreamEvents.Refresh(accountId, personId.toString()))));
    }

    public Result<ImportResult, BusinessError> confirmStatementImport(UUID personId, StatementConfirmCommand cmd) {
        return guards.ownsAccount(cmd.accountId()).flatMap(ignored -> service.confirm(personId, cmd))
                .ifSuccess(ignored -> MessageBus.submit(new AccountStreamEvents.Refresh(cmd.accountId(), personId.toString())));
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
}
