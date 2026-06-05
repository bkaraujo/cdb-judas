package br.community.feature.user.accounts.statement.importer;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._1_application.command.ImportConfirmCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.feature.user.accounts.statement.importer.confirm.BankStatementConfirmCommand;
import br.community.feature.user.accounts.statement.importer.preview.*;
import br.community.feature.user.accounts.transactions.ImportConfirmRequest;
import br.community.feature.user.accounts.transactions.ImportConfirmResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{uuid}/accounts/transactions/import")
public class TransactionImportResource {

    private final StatementImportUseCase statementImport;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) @Nullable String password,
            @RequestParam(value = "accountId", required = false) @Nullable UUID accountId) {

        if (file.isEmpty()) {
            return problem(HttpStatus.UNPROCESSABLE_CONTENT, "FILE_REQUIRED", "Selecione um arquivo PDF.");
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            return problem(HttpStatus.BAD_REQUEST, "FILE_UNREADABLE", "Não foi possível ler o arquivo enviado.");
        }

        return switch (statementImport.preview(bytes, password, accountId)) {
            case Result.Success(var outcome) -> ResponseEntity.<Object>ok(toResponseBody(outcome));
            case Result.Failure(var error) -> problem(error);
        };
    }

    private static Object toResponseBody(ImportPreviewOutcome outcome) {
        return switch (outcome) {
            case ImportPreviewOutcome.Invoice(var preview) -> toResponse(preview);
            case ImportPreviewOutcome.Statement(var preview) -> toStatementResponse(preview);
        };
    }

    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> confirm(@RequestBody @Valid ImportConfirmRequest req) {
        val rows = req.rows().stream().map(TransactionImportResource::toCommandRow).toList();
        val cmd = new ImportConfirmCommand(req.cardId(), rows);

        return switch (statementImport.confirm(cmd)) {
            case Result.Success(var res) ->
                    ResponseEntity.<Object>ok(new ImportConfirmResponse(res.created(), res.reconciled(), res.skipped()));
            case Result.Failure(var error) ->
                    problem(HttpStatus.UNPROCESSABLE_CONTENT, "CARD_NOT_FOUND", messageOf(error));
        };
    }

    @PostMapping(value = "/statement/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> confirmStatement(@RequestBody @Valid BankStatementConfirmRequest req) {
        val rows = req.rows().stream()
                .map(r -> new BankStatementConfirmCommand.Row(r.description(), r.amount(), r.date(), r.type(), r.categoryId()))
                .toList();
        val cmd = new BankStatementConfirmCommand(req.accountId(), rows);

        return switch (statementImport.confirmStatement(cmd)) {
            case Result.Success(var res) ->
                    ResponseEntity.<Object>ok(new ImportConfirmResponse(res.created(), res.reconciled(), res.skipped()));
            case Result.Failure(var error) ->
                    problem(HttpStatus.UNPROCESSABLE_CONTENT, "ACCOUNT_NOT_FOUND", messageOf(error));
        };
    }

    private static ImportConfirmCommand.Row toCommandRow(ImportConfirmRequest.Row row) {
        return new ImportConfirmCommand.Row(
                row.description(), row.amount(), row.date(), row.originalDate(),
                row.installmentNumber(), row.installmentTotal(), row.categoryId());
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
        val rows = preview.rows().stream().map(TransactionImportResource::toRow).toList();
        val matchedCard = preview.matchedCard();
        val matchedCardId = matchedCard != null ? matchedCard.id() : null;
        val candidateCards = preview.candidateCards().stream().map(TransactionImportResource::toCardOption).toList();
        return new ImportPreviewResponse(
                "CREDIT_CARD_INVOICE", preview.issuer().name(), preview.statement().last4s(), rows, matchedCardId, candidateCards);
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

    private static ImportPreviewResponse.CardOption toCardOption(MonetaryAccount card) {
        val last4 = card.additionalInfo().get("last4");
        return new ImportPreviewResponse.CardOption(
                card.id(), card.name(), last4 != null ? String.valueOf(last4) : null);
    }

    private static ImportPreviewResponse.Row toRow(PreviewRow row) {
        final TransactionDraft draft = row.draft();
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
                row.categoryId());
    }

    private static ResponseEntity<Object> problem(ImportError error) {
        return problem(statusFor(error), error.code(), error.message());
    }

    private static HttpStatus statusFor(ImportError error) {
        return switch (error) {
            case ImportError.FileTooLarge ignored -> HttpStatus.CONTENT_TOO_LARGE;
            case ImportError.PasswordRequired ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case ImportError.WrongPassword ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case ImportError.NoTextLayer ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case ImportError.UnknownIssuer ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case ImportError.TooManyPages ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String code, String detail) {
        val pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
