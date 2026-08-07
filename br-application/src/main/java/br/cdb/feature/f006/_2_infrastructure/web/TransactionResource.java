package br.cdb.feature.f006._2_infrastructure.web;

import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.cdb.feature.f006._2_infrastructure.web.request.PatchStatusRequest;
import br.cdb.feature.f006._2_infrastructure.web.request.TransactionRequest;
import br.cdb.feature.f006._2_infrastructure.web.response.TransactionResponse;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionResource {

    private final ReadUseCases reads = Context.tryGet(ReadUseCases.class);
    private final WriteUseCases writes = Context.tryGet(WriteUseCases.class);

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
        return query(uuid, new ReadUseCases.TransactionFilter(null, limit, dateFrom, dateTo, status, type));
    }

    /** Endpoint interno (consumido pelo cliente {@code F006Api} da própria fatia, sobre
     *  {@code InternalApi}, por {@code f005.WriteUseCase} na exclusão de categoria) — público como
     *  qualquer outro em {@code /api/{uuid}/…}, guardado pelo mesmo {@code OwnershipFilter}, nunca
     *  chamado pelo frontend. */
    @GET
    @Path("/transactions/by-category")
    public List<UUID> byCategory(@PathParam("uuid") UUID uuid, @QueryParam("categoryIds") String categoryIds) {
        val ids = categoryIds.isBlank() ? List.<UUID>of()
                : Arrays.stream(categoryIds.split(",")).map(UUID::fromString).toList();
        return reads.transactionIdsByCategories(uuid, ids);
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
        return query(uuid, new ReadUseCases.TransactionFilter(accId, limit, dateFrom, dateTo, status, type));
    }

    @POST
    @Path("/{accId}/transactions")
    public RestResponse<TransactionResponse> create(@PathParam("uuid") UUID uuid, @PathParam("accId") UUID accId, @Valid TransactionRequest req) {
        return switch (writes.createTransaction(uuid, RequestMapper.toCreateCommand(accId, req), req.categoryId(), tagIdsOf(req))) {
            case Result.Success(var transaction) -> RestResponse.status(RestResponse.Status.CREATED, RequestMapper.toDto(transaction));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{accId}/transactions/{txId}")
    public TransactionResponse update(@PathParam("uuid") UUID uuid, @PathParam("accId") UUID accId, @PathParam("txId") UUID txId, @Valid TransactionRequest req) {
        return switch (writes.updateTransaction(uuid, RequestMapper.toUpdateCommand(txId, accId, req), req.categoryId(), tagIdsOf(req))) {
            case Result.Success(var transaction) -> RequestMapper.toDto(transaction);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{accId}/transactions/{txId}/status")
    public TransactionResponse patchStatus(@PathParam("uuid") UUID uuid, @PathParam("txId") UUID txId, @Valid PatchStatusRequest req) {
        return switch (writes.updateTransactionStatus(uuid, txId, req.status(), req.paymentDate())) {
            case Result.Success(var transaction) -> RequestMapper.toDto(transaction);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @DELETE
    @Path("/{accId}/transactions/{txId}")
    public void delete(@PathParam("txId") UUID txId, @QueryParam("mode") @Nullable String mode) {
        if (writes.deleteTransaction(txId, RequestMapper.toScope(mode)) instanceof Result.Failure(var error)) {
            throw new BusinessException(error);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static List<UUID> tagIdsOf(TransactionRequest req) {
        return req.tagIds() != null ? req.tagIds() : List.of();
    }

    private List<TransactionResponse> query(UUID personId, ReadUseCases.TransactionFilter filter) {
        return switch (reads.transactions(personId, filter)) {
            case Result.Success(var transactions) -> transactions.stream().map(RequestMapper::toDto).toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
