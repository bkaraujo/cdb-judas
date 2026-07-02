package br.community.feature.user.accounts.transactions.importer;

import br.commons.Result;
import br.community.context.monetary._1_application.command.ImportConfirmCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.core.web.error.ProblemDetail;
import br.community.feature.user.accounts.transactions.importer.confirm.BankStatementConfirmCommand;
import br.community.feature.user.accounts.transactions.importer.confirm.ImportConfirmResponse;
import br.community.feature.user.accounts.transactions.importer.preview.*;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts/transactions/import")
@RequiredArgsConstructor
public class StatementImportResource {

    private final StatementImportUseCase statementImport;

    @POST
    @Path("/preview")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response preview(
            @RestForm("file") @Nullable FileUpload file,
            @RestForm @Nullable String password,
            @RestForm @Nullable UUID accountId) {

        if (file == null || file.size() == 0) {
            return problem422("FILE_REQUIRED", "Selecione um arquivo PDF.");
        }

        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.filePath());
        } catch (IOException e) {
            return problem(Response.Status.BAD_REQUEST, "FILE_UNREADABLE", "Não foi possível ler o arquivo enviado.");
        }

        return switch (statementImport.preview(bytes, password, accountId)) {
            case Result.Success(var outcome) -> Response.ok(toResponseBody(outcome)).build();
            case Result.Failure(var error) -> problem(error);
        };
    }

    private static Object toResponseBody(ImportPreviewOutcome outcome) {
        return switch (outcome) {
            case ImportPreviewOutcome.Invoice(var preview) -> toResponse(preview);
            case ImportPreviewOutcome.Statement(var preview) -> toStatementResponse(preview);
        };
    }

    @POST
    @Path("/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response confirm(@Valid StatementConfirmRequest req) {
        return switch (req.type()) {
            case CREDIT_CARD_INVOICE -> confirmInvoice(req);
            case BANK_STATEMENT -> confirmBankStatement(req);
        };
    }

    private Response confirmInvoice(StatementConfirmRequest req) {
        if (req.rows().stream().anyMatch(row -> row.cardId() == null)) {
            return problem422("CARD_REQUIRED", "Cada lançamento precisa de um cartão de destino.");
        }
        val rows = req.rows().stream().map(StatementImportResource::toInvoiceRow).toList();
        val cmd = new ImportConfirmCommand(rows);

        return switch (statementImport.confirm(cmd)) {
            case Result.Success(var res) ->
                    Response.ok(new ImportConfirmResponse(res.created(), res.reconciled(), res.skipped())).build();
            case Result.Failure(var error) -> problem422("CARD_NOT_FOUND", messageOf(error));
        };
    }

    private Response confirmBankStatement(StatementConfirmRequest req) {
        if (req.accountId() == null) {
            return problem422("ACCOUNT_REQUIRED", "O campo accountId é obrigatório para extratos bancários.");
        }
        val rows = req.rows().stream()
                .map(r -> new BankStatementConfirmCommand.Row(r.description(), r.amount(), r.date(), r.transactionType(), r.categoryId()))
                .toList();
        val cmd = new BankStatementConfirmCommand(req.accountId(), rows);

        return switch (statementImport.confirmStatement(cmd)) {
            case Result.Success(var res) ->
                    Response.ok(new ImportConfirmResponse(res.created(), res.reconciled(), res.skipped())).build();
            case Result.Failure(var error) -> problem422("ACCOUNT_NOT_FOUND", messageOf(error));
        };
    }

    /** {@code cardId} is guaranteed non-null by the {@code CARD_REQUIRED} guard in {@link #confirmInvoice}. */
    private static ImportConfirmCommand.Row toInvoiceRow(StatementConfirmRequest.Row row) {
        return new ImportConfirmCommand.Row(
                row.description(), row.amount(), row.date(), row.originalDate(),
                row.installmentNumber(), row.installmentTotal(), row.categoryId(),
                Objects.requireNonNull(row.cardId()));
    }

    private static String messageOf(DomainError error) {
        return switch (error) {
            case DomainError.NotFound(var message) -> message;
            case DomainError.BusinessRule(var message) -> message;
            case DomainError.Validation(var message) -> message;
            case DomainError.Conflict(var message) -> message;
        };
    }

    private static ImportPreviewResponse toResponse(ImportPreview preview) {
        val rows = preview.rows().stream().map(StatementImportResource::toRow).toList();
        val candidateCards = preview.candidateCards().stream().map(StatementImportResource::toCardOption).toList();
        return new ImportPreviewResponse(
                "CREDIT_CARD_INVOICE", preview.issuer().name(), preview.last4s(), rows, candidateCards);
    }

    private static BankStatementPreviewResponse toStatementResponse(BankStatementPreview preview) {
        val accounts = preview.candidateAccounts().stream()
                .map(a -> new BankStatementPreviewResponse.AccountOption(a.id(), a.name()))
                .toList();
        val rows = preview.rows().stream()
                .map(r -> new BankStatementPreviewResponse.Row(
                        r.date().toString(), r.description(), r.amount(), r.type(),
                        r.state().name(), r.categoryId(), r.reconcileDescription()))
                .toList();
        return new BankStatementPreviewResponse(
                "BANK_STATEMENT", preview.issuer().name(), accounts, preview.selectedAccountId(), rows);
    }

    private static ImportPreviewResponse.CardOption toCardOption(CreditCard card) {
        return new ImportPreviewResponse.CardOption(card.id(), card.accountName(), card.last4());
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
                draft.status(),
                row.duplicate(),
                row.categoryId(),
                row.suggestedCardId());
    }

    private static Response problem(ImportError error) {
        if (error instanceof ImportError.FileTooLarge) {
            return problem(413, "Content Too Large", error.code(), error.message());
        }
        return problem422(error.code(), error.message());
    }

    private static Response problem422(String code, String detail) {
        return problem(422, "Unprocessable Content", code, detail);
    }

    private static Response problem(Response.Status status, String code, String detail) {
        return problem(status.getStatusCode(), status.getReasonPhrase(), code, detail);
    }

    private static Response problem(int status, String reason, String code, String detail) {
        val pd = ProblemDetail.of(status, reason, null, detail).withCode(code);
        return Response.status(status).entity(pd).build();
    }
}
