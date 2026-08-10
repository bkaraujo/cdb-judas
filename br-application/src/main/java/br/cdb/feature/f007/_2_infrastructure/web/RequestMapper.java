package br.cdb.feature.f007._2_infrastructure.web;

import br.cdb.feature.f000._0_domain.ClosedPeriod;
import br.cdb.feature.f003.F003Api;
import br.cdb.feature.f007._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f007._1_application.preview.BankStatementPreview;
import br.cdb.feature.f007._1_application.preview.ImportPreview;
import br.cdb.feature.f007._1_application.preview.ImportPreviewOutcome;
import br.cdb.feature.f007._1_application.preview.PreviewRow;
import br.cdb.feature.f007._1_application.usecase.ImportUseCase;
import br.cdb.feature.f007._2_infrastructure.web.request.StatementConfirmRequest;
import br.cdb.feature.f007._2_infrastructure.web.response.BankStatementPreviewResponse;
import br.cdb.feature.f007._2_infrastructure.web.response.ImportPreviewResponse;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@NullMarked
public abstract class RequestMapper {
    private RequestMapper() {}

    static Object toResponseBody(ImportUseCase.ImportPreviewView view) {
        return switch (view.outcome()) {
            case ImportPreviewOutcome.Invoice(var preview) ->
                    RequestMapper.toResponse(preview, view.accountNamesById(), view.closedPeriod());
            case ImportPreviewOutcome.Statement(var preview) ->
                    RequestMapper.toStatementResponse(preview, view.closedPeriod());
        };
    }

    static ImportPreviewResponse toResponse(ImportPreview preview, Map<UUID, String> accountNames, ClosedPeriod closed) {
        val rows = preview.rows().stream().map(row -> toRow(row, closed)).toList();
        val candidateCards = preview.candidateCards().stream()
                .map(card -> toCardOption(card, accountNames)).toList();
        return new ImportPreviewResponse("CREDIT_CARD_INVOICE", preview.issuer(), preview.last4s(), rows, candidateCards,
                closed.period() == null ? null : closed.label());
    }

    static BankStatementPreviewResponse toStatementResponse(BankStatementPreview preview, ClosedPeriod closed) {
        val accounts = preview.candidateAccounts().stream()
                .map(a -> new BankStatementPreviewResponse.AccountOption(a.id(), a.name()))
                .toList();
        val rows = preview.rows().stream()
                .map(r -> new BankStatementPreviewResponse.Row(
                        r.date().toString(), r.description(), r.amount(), r.type(),
                        r.state().name(), closed.covers(r.date()), r.categoryId(), r.costCenterId(), r.reconcileDescription()))
                .toList();
        return new BankStatementPreviewResponse(
                "BANK_STATEMENT", preview.issuer(), accounts, preview.selectedAccountId(), rows,
                closed.period() == null ? null : closed.label());
    }

    private static ImportPreviewResponse.CardOption toCardOption(F003Api.CardView card, Map<UUID, String> accountNames) {
        return new ImportPreviewResponse.CardOption(card.id(), accountNames.getOrDefault(card.accountId(), ""), card.last4());
    }

    private static ImportPreviewResponse.Row toRow(PreviewRow row, ClosedPeriod closed) {
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
                draft.type(),
                row.duplicate(),
                closed.covers(draft.date()),
                row.categoryId(),
                row.costCenterId(),
                row.suggestedCardId());
    }

    static InvoiceConfirmCommand.Row toInvoiceRow(StatementConfirmRequest.Row row) {
        return new InvoiceConfirmCommand.Row(
                row.description(), row.amount(), row.date(), row.originalDate(),
                row.installmentNumber(), row.installmentTotal(), row.transactionType(), row.categoryId(), row.costCenterId(),
                Objects.requireNonNull(row.cardId()), row.tagIds() != null ? row.tagIds() : List.of());
    }
}
