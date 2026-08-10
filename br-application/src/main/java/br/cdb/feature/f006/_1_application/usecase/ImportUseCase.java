package br.cdb.feature.f006._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.ClosedPeriod;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002.F002Api;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._1_application.usecase.ReadUseCase;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f004.F004Api;
import br.cdb.feature.f006._0_domain.ImportError;
import br.cdb.feature.f006._0_domain.ImportResult;
import br.cdb.feature.f006._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f006._1_application.confirm.StatementConfirmCommand;
import br.cdb.feature.f006._1_application.preview.ImportPreviewOutcome;
import br.cdb.feature.f006._1_application.service.StatementImportService;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Use case de importação de extrato/fatura (f007 fundida em f006 na fase 6 de {@code .claude/plan.md}).
 * SSE de conta publica {@link AccountStreamEvents.Refresh} — dispatch é responsabilidade única de
 * {@code f999}.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class ImportUseCase {

    private final ReadUseCase accountReads = Context.tryGet(ReadUseCase.class);
    // FQN: o ReadUseCase de f003 tem o mesmo nome simples do de f002, importado acima.
    private final br.cdb.feature.f003._1_application.usecase.ReadUseCase cardReads =
            Context.tryGet(br.cdb.feature.f003._1_application.usecase.ReadUseCase.class);

    private final StatementImportService service = Context.get(StatementImportService.class);
    /** Cliente da API pública de f002 — o fechamento contábil é dela. */
    private final F002Api f002 = Context.tryGet(F002Api.class);
    /** Cliente da API pública de f004 — a posse da tag é dela. */
    private final F004Api f004 = Context.tryGet(F004Api.class);

    private final UserGuards guards;

    /**
     * Preview + nomes de conta por personId (rotulam as opções de cartão na resposta) + o fechamento
     * contábil vigente, que marca no diálogo as linhas que a importação vai recusar.
     */
    @NullMarked
    public record ImportPreviewView(ImportPreviewOutcome outcome, Map<UUID, String> accountNamesById,
                                    ClosedPeriod closedPeriod) {}

    public Result<ImportPreviewView, ImportError> importPreview(byte[] fileBytes, @Nullable String password, @Nullable UUID accountId) {
        if (accountId != null && guards.ownsAccount(accountId).isFailure()) {
            return new Result.Failure<>(new ImportError.AccountNotFound());
        }

        return service.preview(HTTPRequest.personId(), fileBytes, password, accountId)
                .map(outcome -> new ImportPreviewView(outcome, accountNamesById(), f002.closingPeriod()));
    }

    public Result<ImportResult, BusinessError> confirmInvoiceImport(UUID personId, InvoiceConfirmCommand cmd) {
        for (val row : cmd.rows()) {
            if (guards.ownsCard(row.cardId()) instanceof Result.Failure<Void, BusinessError>(var error)) {
                return Result.failure(error);
            }
        }
        return validateClosing(cmd.rows().stream().map(InvoiceConfirmCommand.Row::date).toList())
                .flatMap(ignored -> f004.ownsTags(tagIdsOf(cmd.rows().stream().map(InvoiceConfirmCommand.Row::tagIds).toList())))
                .flatMap(ignored -> service.confirm(personId, cmd))
                .ifSuccess(ignored -> affectedAccountIds(cmd.rows(), personId.toString())
                        .forEach(accountId -> MessageBus.submit(new AccountStreamEvents.Refresh(accountId, personId.toString()))));
    }

    public Result<ImportResult, BusinessError> confirmStatementImport(UUID personId, StatementConfirmCommand cmd) {
        return guards.ownsAccount(cmd.accountId())
                .flatMap(ignored -> validateClosing(cmd.rows().stream().map(StatementConfirmCommand.Row::date).toList()))
                .flatMap(ignored -> f004.ownsTags(tagIdsOf(cmd.rows().stream().map(StatementConfirmCommand.Row::tagIds).toList())))
                .flatMap(ignored -> service.confirm(personId, cmd))
                .ifSuccess(ignored -> MessageBus.submit(new AccountStreamEvents.Refresh(cmd.accountId(), personId.toString())));
    }

    /**
     * Política de usuário na entrada da fatia, como a de {@code WriteUseCases}: nenhuma linha do
     * documento pode cair no período fechado. Recusa o lote inteiro <b>antes</b> de qualquer escrita —
     * o diálogo já marca essas linhas com aviso e as deixa fora da seleção, então só chega aqui quem
     * confirmou com um preview vencido (ou chamou a API direto).
     */
    private Result<Void, BusinessError> validateClosing(List<LocalDate> dates) {
        val closed = f002.closingPeriod();
        for (val date : dates) {
            if (closed.covers(date)) {
                return Result.failure(new BusinessError.BusinessRule(
                        "Período fechado. Lançamentos até " + closed.label() + " não podem ser importados."));
            }
        }
        return Result.success();
    }

    /** Tags distintas de todas as linhas confirmadas — uma guarda de posse só para o lote inteiro. */
    private static Set<UUID> tagIdsOf(List<List<UUID>> rowTagIds) {
        return rowTagIds.stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    /** Nome da conta a que cada cartão pertence, para rotular as opções de cartão do preview. */
    private Map<UUID, String> accountNamesById() {
        return accountReads.listAccounts(HTTPRequest.personId()).getOrElse(List.of()).stream()
                .collect(Collectors.toMap(Account::id, Account::name));
    }

    /** Contas distintas donas dos cartões das linhas confirmadas. */
    private List<UUID> affectedAccountIds(List<InvoiceConfirmCommand.Row> rows, String personId) {
        val accountByCard = cardReads.list(personId).getOrElse(List.of()).stream()
                .collect(Collectors.toMap(CreditCard::id, CreditCard::accountId));
        return rows.stream()
                .map(row -> accountByCard.get(row.cardId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
