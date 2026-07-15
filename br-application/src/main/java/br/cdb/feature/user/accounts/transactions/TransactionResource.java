package br.cdb.feature.user.accounts.transactions;

import br.cdb.feature.user.UserUseCase;
import br.cdb.feature.user.accounts.transactions.core.TransactionMapper;
import br.commons.Result;
import br.commons.business.BusinessException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TransactionResource {

    private final UserUseCase userUseCase;

    // ── Cross-account collection ───────────────────────────────────

    @GET
    @Path("/transactions")
    public List<TransactionResponse> listAll(
            @PathParam("uuid") UUID uuid,
            @QueryParam("limit") @Nullable Integer limit,
            @QueryParam("dateFrom") @Nullable LocalDate dateFrom,
            @QueryParam("dateTo") @Nullable LocalDate dateTo,
            @QueryParam("status") @Nullable String status,
            @QueryParam("type") @Nullable String type
    ) {
        return query(uuid, new UserUseCase.TransactionFilter(null, limit, dateFrom, dateTo, status, type));
    }

    // ── Per-account collection + items ─────────────────────────────

    @GET
    @Path("/{accId}/transactions")
    public List<TransactionResponse> listByAccount(
            @PathParam("uuid") UUID uuid,
            @PathParam("accId") UUID accId,
            @QueryParam("limit") @Nullable Integer limit,
            @QueryParam("dateFrom") @Nullable LocalDate dateFrom,
            @QueryParam("dateTo") @Nullable LocalDate dateTo,
            @QueryParam("status") @Nullable String status,
            @QueryParam("type") @Nullable String type
    ) {
        return query(uuid, new UserUseCase.TransactionFilter(accId, limit, dateFrom, dateTo, status, type));
    }

    @POST
    @Path("/{accId}/transactions")
    public RestResponse<TransactionResponse> create(@PathParam("uuid") UUID uuid, @PathParam("accId") UUID accId, @Valid TransactionRequest req) {
        return switch (userUseCase.createTransaction(uuid, TransactionMapper.toCreateCommand(accId, req), req.categoryId())) {
            case Result.Success(var view) -> RestResponse.status(RestResponse.Status.CREATED, toDto(view));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{accId}/transactions/{txId}")
    public TransactionResponse update(@PathParam("uuid") UUID uuid, @PathParam("accId") UUID accId, @PathParam("txId") UUID txId, @Valid TransactionRequest req) {
        return switch (userUseCase.updateTransaction(uuid, TransactionMapper.toUpdateCommand(txId, accId, req), req.categoryId())) {
            case Result.Success(var view) -> toDto(view);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{accId}/transactions/{txId}/status")
    public TransactionResponse patchStatus(@PathParam("uuid") UUID uuid, @PathParam("txId") UUID txId, @Valid PatchStatusRequest req) {
        return switch (userUseCase.updateTransactionStatus(uuid, txId, req.status(), req.paymentDate())) {
            case Result.Success(var view) -> toDto(view);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @DELETE
    @Path("/{accId}/transactions/{txId}")
    public void delete(@PathParam("txId") UUID txId, @QueryParam("mode") @Nullable String mode) {
        if (userUseCase.deleteTransaction(txId, TransactionMapper.toScope(mode)) instanceof Result.Failure(var error)) {
            throw new BusinessException(error);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private List<TransactionResponse> query(UUID userId, UserUseCase.TransactionFilter filter) {
        return switch (userUseCase.transactions(userId, filter)) {
            case Result.Success(var views) -> views.stream().map(TransactionResource::toDto).toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    private static TransactionResponse toDto(UserUseCase.TransactionView view) {
        return TransactionMapper.toDto(view.transaction(), view.overlay());
    }
}
