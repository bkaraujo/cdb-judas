package br.community.feature.user.accounts.transactions;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.transactions.core.AbstractResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionResource extends AbstractResource {

    private final MonetaryContext monetaryContext;

    // ── Cross-account collection ───────────────────────────────────

    @GetMapping("/transactions")
    public List<TransactionResponse> listAll(
            @Nullable @RequestParam(required = false) Integer limit,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Nullable @RequestParam(required = false) String status,
            @Nullable @RequestParam(required = false) String type
    ) {
        return query(null, limit, dateFrom, dateTo, status, type);
    }

    // ── Per-account collection + items ─────────────────────────────

    @GetMapping("/{accId}/transactions")
    public List<TransactionResponse> listByAccount(
            @PathVariable UUID accId,
            @Nullable @RequestParam(required = false) Integer limit,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Nullable @RequestParam(required = false) String status,
            @Nullable @RequestParam(required = false) String type
    ) {
        return query(accId, limit, dateFrom, dateTo, status, type);
    }

    @PostMapping("/{accId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@PathVariable UUID accId, @RequestBody @Valid TransactionRequest req) {
        return switch (monetaryContext.createTransaction(toCommand(accId, req))) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{accId}/transactions/{txId}")
    public TransactionResponse update(@PathVariable UUID accId, @PathVariable UUID txId, @RequestBody @Valid TransactionRequest req) {
        return switch (monetaryContext.updateTransaction(txId, toCommand(accId, req))) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{accId}/transactions/{txId}/status")
    public TransactionResponse patchStatus(@PathVariable UUID txId, @RequestBody @Valid PatchStatusRequest req) {
        return switch (monetaryContext.updateTransactionStatus(txId, req.status(), req.paymentDate())) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DeleteMapping("/{accId}/transactions/{txId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID txId, @Nullable @RequestParam(required = false) String mode) {
        if (monetaryContext.deleteTransaction(txId, mode) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
    }

    // ── Shared query + mapping ─────────────────────────────────────

    private List<TransactionResponse> query(
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
                yield transactions.stream().map(this::toDto).toList();
            }
        };
    }
}
