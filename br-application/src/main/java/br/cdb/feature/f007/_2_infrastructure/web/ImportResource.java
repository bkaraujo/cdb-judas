package br.cdb.feature.f007._2_infrastructure.web;

import br.cdb.core.web.HTTPRequest;
import br.cdb.core.web.HTTPResponse;
import br.cdb.feature.f007._0_domain.ImportError;
import br.cdb.feature.f007._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f007._1_application.confirm.StatementConfirmCommand;
import br.cdb.feature.f007._1_application.usecase.ImportUseCase;
import br.cdb.feature.f007._2_infrastructure.web.request.StatementConfirmRequest;
import br.cdb.feature.f007._2_infrastructure.web.response.ImportConfirmResponse;
import br.commons.Logger;
import br.commons.Result;
import br.commons.business.BusinessError;
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
import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts/transactions/import")
@RequiredArgsConstructor
public class ImportResource {

    private final ImportUseCase importUseCase;

    @POST
    @Path("/preview")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response preview(
            @RestForm("file") @Nullable FileUpload file,
            @RestForm @Nullable String password,
            @RestForm @Nullable UUID accountId) {

        if (file == null || file.size() == 0) {
            return HTTPResponse.unprocessable("FILE_REQUIRED", "Selecione um arquivo PDF.");
        }

        final byte[] bytes;
        try {
            Logger.debug("Importing statement %s", file.uploadedFile());
            bytes = Files.readAllBytes(file.filePath());
        } catch (IOException e) {
            return HTTPResponse.badRequest("FILE_UNREADABLE", "Não foi possível ler o arquivo enviado.");
        }

        return switch (importUseCase.importPreview(bytes, password, accountId)) {
            case Result.Success(var view) -> Response.ok(RequestMapper.toResponseBody(view)).build();
            case Result.Failure(var error) -> {
                if (error instanceof ImportError.FileTooLarge) {
                    yield HTTPResponse.tooLarge(error.code(), error.message());
                }

                yield HTTPResponse.unprocessable(error.code(), error.message());
            }
        };
    }

    @POST
    @Path("/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response confirm(@Valid StatementConfirmRequest req) {
        val personId = UUID.fromString(HTTPRequest.personId());
        return switch (req.type()) {
            case CREDIT_CARD_INVOICE -> confirmInvoice(personId, req);
            case BANK_STATEMENT -> confirmBankStatement(personId, req);
        };
    }

    private Response confirmInvoice(UUID personId, StatementConfirmRequest req) {
        if (req.rows().stream().anyMatch(row -> row.cardId() == null)) {
            return HTTPResponse.unprocessable("CARD_REQUIRED", "Cada lançamento precisa de um cartão de destino.");
        }
        val rows = req.rows().stream().map(RequestMapper::toInvoiceRow).toList();

        return switch (importUseCase.confirmInvoiceImport(personId, new InvoiceConfirmCommand(rows))) {
            case Result.Success(var res) ->
                    Response.ok(new ImportConfirmResponse(res.created(), res.reconciled(), res.skipped())).build();
            case Result.Failure(var error) -> failure(error, "CARD_NOT_FOUND");
        };
    }

    private Response confirmBankStatement(UUID personId, StatementConfirmRequest req) {
        if (req.accountId() == null) {
            return HTTPResponse.unprocessable("ACCOUNT_REQUIRED", "O campo accountId é obrigatório para extratos bancários.");
        }
        val rows = req.rows().stream()
                .map(r -> new StatementConfirmCommand.Row(r.description(), r.amount(), r.date(), r.transactionType(), r.categoryId(), r.costCenterId(),
                        r.tagIds() != null ? r.tagIds() : List.of()))
                .toList();

        return switch (importUseCase.confirmStatementImport(personId, new StatementConfirmCommand(req.accountId(), rows))) {
            case Result.Success(var res) ->
                    Response.ok(new ImportConfirmResponse(res.created(), res.reconciled(), res.skipped())).build();
            case Result.Failure(var error) -> failure(error, "ACCOUNT_NOT_FOUND");
        };
    }

    /** Período fechado é a única regra de negócio que o confirm devolve — ganha código próprio pro
     *  diálogo tratar; o resto é entidade não encontrada (cartão/conta). */
    private static Response failure(BusinessError error, String notFoundCode) {
        val code = error instanceof BusinessError.BusinessRule ? "CLOSED_PERIOD" : notFoundCode;
        return HTTPResponse.unprocessable(code, messageOf(error));
    }

    private static String messageOf(BusinessError error) {
        return switch (error) {
            case BusinessError.NotFound(var message) -> message;
            case BusinessError.BusinessRule(var message) -> message;
            case BusinessError.Validation(var message) -> message;
            case BusinessError.Conflict(var message) -> message;
        };
    }

}
