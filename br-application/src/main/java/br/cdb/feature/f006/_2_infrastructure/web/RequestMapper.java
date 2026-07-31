package br.cdb.feature.f006._2_infrastructure.web;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.command.TransactionCommand;
import br.cdb.feature.f006._1_application.command.TransactionScope;
import br.cdb.feature.f006._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f006._1_application.preview.BankStatementPreview;
import br.cdb.feature.f006._1_application.preview.ImportPreview;
import br.cdb.feature.f006._1_application.preview.ImportPreviewOutcome;
import br.cdb.feature.f006._1_application.preview.PreviewRow;
import br.cdb.feature.f006._1_application.usecase.ImportUseCase;
import br.cdb.feature.f006._2_infrastructure.web.request.StatementConfirmRequest;
import br.cdb.feature.f006._2_infrastructure.web.request.TransactionRequest;
import br.cdb.feature.f006._2_infrastructure.web.response.BankStatementPreviewResponse;
import br.cdb.feature.f006._2_infrastructure.web.response.ImportPreviewResponse;
import br.cdb.feature.f006._2_infrastructure.web.response.TransactionResponse;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@NullMarked
public abstract class RequestMapper {
    private RequestMapper(){}
    public static TransactionResponse toDto(Transaction t) {
        return new TransactionResponse(
                t.id(),
                t.description(),
                BigDecimal.valueOf(t.signal()).multiply(t.amount()),
                t.date(),
                t.categoryId(),
                t.accountId(),
                t.status(),
                t.type(),
                t.costCenterId(),
                t.paymentDate(),
                t.groupId(),
                t.installmentNumber(),
                t.totalInstallments(),
                t.notes(),
                t.cardId()
        );
    }

    public static TransactionCommand.Create toCreateCommand(UUID accId, TransactionRequest req) {
        return new TransactionCommand.Create(
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.costCenterId(),
                req.status(),
                req.type(),
                req.installments(),
                req.notes(),
                req.cardId()
        );
    }

    public static TransactionCommand.Update toUpdateCommand(UUID txId, UUID accId, TransactionRequest req) {
        return new TransactionCommand.Update(
                txId,
                req.description(),
                req.amount(),
                req.date(),
                accId,
                req.costCenterId(),
                req.status(),
                req.type(),
                toScope(req.editMode()),
                req.notes(),
                req.cardId()
        );
    }

    /** Qualquer valor além de {@code "FUTURE"} (inclusive {@code null}) vira {@link TransactionScope.Single}. */
    public static TransactionScope toScope(@Nullable String mode) {
        return "FUTURE".equalsIgnoreCase(mode) ? new TransactionScope.Future() : new TransactionScope.Single();
    }

    static Object toResponseBody(ImportUseCase.ImportPreviewView view) {
        return switch (view.outcome()) {
            case ImportPreviewOutcome.Invoice(var preview) -> RequestMapper.toResponse(preview, view.accountNamesById());
            case ImportPreviewOutcome.Statement(var preview) -> RequestMapper.toStatementResponse(preview);
        };
    }

    static ImportPreviewResponse toResponse(ImportPreview preview, Map<UUID, String> accountNames) {
        val rows = preview.rows().stream().map(RequestMapper::toRow).toList();
        val candidateCards = preview.candidateCards().stream()
                .map(card -> toCardOption(card, accountNames)).toList();
        return new ImportPreviewResponse("CREDIT_CARD_INVOICE", preview.issuer(), preview.last4s(), rows, candidateCards);
    }

    static BankStatementPreviewResponse toStatementResponse(BankStatementPreview preview) {
        val accounts = preview.candidateAccounts().stream()
                .map(a -> new BankStatementPreviewResponse.AccountOption(a.id(), a.name()))
                .toList();
        val rows = preview.rows().stream()
                .map(r -> new BankStatementPreviewResponse.Row(
                        r.date().toString(), r.description(), r.amount(), r.type(),
                        r.state().name(), r.categoryId(), r.reconcileDescription()))
                .toList();
        return new BankStatementPreviewResponse(
                "BANK_STATEMENT", preview.issuer(), accounts, preview.selectedAccountId(), rows);
    }

    private static ImportPreviewResponse.CardOption toCardOption(CreditCard card, Map<UUID, String> accountNames) {
        return new ImportPreviewResponse.CardOption(card.id(), accountNames.getOrDefault(card.accountId(), ""), card.last4());
    }

    private static ImportPreviewResponse.Row toRow(PreviewRow row) {
        val draft = row.draft();
        return new ImportPreviewResponse.Row(
                draft.last4(),
                draft.date().toString(),
                draft.originalDate().toString(),
                draft.description(),
                draft.amount(),
                draft.installmentNumber(),
                draft.installmentTotal(),
                draft.groupId(),
                draft.status(),
                row.duplicate(),
                row.categoryId(),
                row.suggestedCardId());
    }

    static InvoiceConfirmCommand.Row toInvoiceRow(StatementConfirmRequest.Row row) {
        return new InvoiceConfirmCommand.Row(
                row.description(), row.amount(), row.date(), row.originalDate(),
                row.installmentNumber(), row.installmentTotal(), row.categoryId(),
                Objects.requireNonNull(row.cardId()));
    }
}
