package br.community.feature.user.accounts.transactions;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.accounts.transactions.core.TransactionMapper;
import br.community.feature.user.tags.UserTransactionTagService;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
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

    private final MonetaryContext monetaryContext;
    private final ClosingService closingService;
    private final UserTransactionService userTransactionService;
    private final UserTransactionTagService tagLinkService;

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
        return query(uuid, null, limit, dateFrom, dateTo, status, type);
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
        return query(uuid, accId, limit, dateFrom, dateTo, status, type);
    }

    @POST
    @Path("/{accId}/transactions")
    public RestResponse<TransactionResponse> create(@PathParam("uuid") UUID uuid, @PathParam("accId") UUID accId, @Valid TransactionRequest req) {
        guardClosing(req.date());
        return switch (monetaryContext.createTransaction(TransactionMapper.toCommand(accId, req))) {
            case Result.Success(var t) -> {
                // Create USER_TRANSACTION for first installment, then for group siblings
                val ut = saveUserTransaction(t, uuid, req.categoryId());
                saveUserTransactionForGroup(t, uuid, req.categoryId());
                yield RestResponse.status(RestResponse.Status.CREATED, TransactionMapper.toDto(t, ut));
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PATCH
    @Path("/{accId}/transactions/{txId}")
    public TransactionResponse update(@PathParam("uuid") UUID uuid, @PathParam("accId") UUID accId, @PathParam("txId") UUID txId, @Valid TransactionRequest req) {
        UUID previousAccountId = null;
        if (monetaryContext.findTransaction(txId) instanceof Result.Success(var existing)) {
            guardClosing(existing.date());
            guardClosing(req.date());
            previousAccountId = existing.accountId();
        }
        return switch (monetaryContext.updateTransaction(txId, TransactionMapper.toCommand(accId, req))) {
            case Result.Success(var t) -> {
                // USER_TRANSACTION's PK now includes the account: if this update moved the
                // transaction to a different account, the old composite-key row would otherwise
                // be orphaned (save() below would INSERT a new row instead of UPDATE-in-place).
                if (previousAccountId != null && !previousAccountId.equals(t.accountId())) {
                    userTransactionService.deleteByTransactionAccountAndUser(t.id(), previousAccountId, uuid);
                }
                val existing = userTransactionService.find(t.id(), t.accountId(), uuid);
                val ut = userTransactionService.save(t.id(), t.accountId(), uuid, req.categoryId());
                // If installment group: update category for all group members
                if (t.groupId() != null && existing.isEmpty()) {
                    saveUserTransactionForGroup(t, uuid, req.categoryId());
                }
                yield TransactionMapper.toDto(t, ut);
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PATCH
    @Path("/{accId}/transactions/{txId}/status")
    public TransactionResponse patchStatus(@PathParam("uuid") UUID uuid, @PathParam("txId") UUID txId, @Valid PatchStatusRequest req) {
        return switch (monetaryContext.updateTransactionStatus(txId, req.status(), req.paymentDate())) {
            case Result.Success(var t) -> {
                val ut = userTransactionService.find(t.id(), t.accountId(), uuid).orElse(null);
                yield TransactionMapper.toDto(t, ut);
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DELETE
    @Path("/{accId}/transactions/{txId}")
    public void delete(@PathParam("txId") UUID txId, @QueryParam("mode") @Nullable String mode) {
        if (monetaryContext.findTransaction(txId) instanceof Result.Success(var existing)) {
            guardClosing(existing.date());
        }
        switch (monetaryContext.deleteTransaction(txId, mode)) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var ids) -> ids.forEach(id -> {
                userTransactionService.deleteByTransaction(id);
                tagLinkService.deleteByTransaction(id);
            });
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void guardClosing(LocalDate date) {
        if (closingService.validateDate(date) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
    }

    private UserTransaction saveUserTransaction(Transaction t, UUID userId, @Nullable UUID categoryId) {
        return userTransactionService.save(t.id(), t.accountId(), userId, categoryId);
    }

    private void saveUserTransactionForGroup(Transaction first, UUID userId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        monetaryContext.listTransactions().getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> userTransactionService.save(t.id(), t.accountId(), userId, categoryId));
    }

    private List<TransactionResponse> query(
            UUID userId,
            @Nullable UUID accId,
            @Nullable Integer limit,
            @Nullable LocalDate dateFrom,
            @Nullable LocalDate dateTo,
            @Nullable String status,
            @Nullable String type
    ) {
        return switch (monetaryContext.listTransactions()) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var all) -> {
                val userTxMap = userTransactionService.indexByTransaction(userId);
                val filtered = all.stream()
                        .filter(t -> accId == null || accId.equals(t.accountId()))
                        .filter(t -> dateFrom == null || !t.date().isBefore(dateFrom))
                        .filter(t -> dateTo == null || !t.date().isAfter(dateTo))
                        .filter(t -> status == null || status.equalsIgnoreCase(t.status().name()))
                        .filter(t -> type == null || type.equalsIgnoreCase(t.type().name()))
                        .toList();
                val transactions = (limit != null && limit > 0 && limit < filtered.size())
                        ? filtered.subList(0, limit)
                        : filtered;
                yield transactions.stream()
                        .map(t -> TransactionMapper.toDto(t, userTxMap.get(t.id())))
                        .toList();
            }
        };
    }
}
