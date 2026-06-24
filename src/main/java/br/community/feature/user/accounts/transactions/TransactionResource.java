package br.community.feature.user.accounts.transactions;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.accounts.transactions.core.TransactionMapper;
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
public class TransactionResource {

    private final MonetaryContext monetaryContext;
    private final ClosingService closingService;
    private final UserTransactionService userTransactionService;

    // ── Cross-account collection ───────────────────────────────────

    @GetMapping("/transactions")
    public List<TransactionResponse> listAll(
            @PathVariable UUID uuid,
            @Nullable @RequestParam(required = false) Integer limit,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Nullable @RequestParam(required = false) String status,
            @Nullable @RequestParam(required = false) String type
    ) {
        return query(uuid, null, limit, dateFrom, dateTo, status, type);
    }

    // ── Per-account collection + items ─────────────────────────────

    @GetMapping("/{accId}/transactions")
    public List<TransactionResponse> listByAccount(
            @PathVariable UUID uuid,
            @PathVariable UUID accId,
            @Nullable @RequestParam(required = false) Integer limit,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Nullable @RequestParam(required = false) String status,
            @Nullable @RequestParam(required = false) String type
    ) {
        return query(uuid, accId, limit, dateFrom, dateTo, status, type);
    }

    @PostMapping("/{accId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@PathVariable UUID uuid, @PathVariable UUID accId, @RequestBody @Valid TransactionRequest req) {
        guardClosing(req.date());
        return switch (monetaryContext.createTransaction(TransactionMapper.toCommand(accId, req))) {
            case Result.Success(var t) -> {
                // Create USER_TRANSACTION for first installment, then for group siblings
                val ut = saveUserTransaction(t, uuid, req.categoryId());
                saveUserTransactionForGroup(t, uuid, req.categoryId());
                yield TransactionMapper.toDto(t, ut);
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{accId}/transactions/{txId}")
    public TransactionResponse update(@PathVariable UUID uuid, @PathVariable UUID accId, @PathVariable UUID txId, @RequestBody @Valid TransactionRequest req) {
        if (monetaryContext.findTransaction(txId) instanceof Result.Success(var existing)) {
            guardClosing(existing.date());
            guardClosing(req.date());
        }
        return switch (monetaryContext.updateTransaction(txId, TransactionMapper.toCommand(accId, req))) {
            case Result.Success(var t) -> {
                val existing = userTransactionService.find(t.id(), uuid);
                val ut = userTransactionService.save(t.id(), uuid, req.categoryId());
                // If installment group: update category for all group members
                if (t.groupId() != null && existing.isEmpty()) {
                    saveUserTransactionForGroup(t, uuid, req.categoryId());
                }
                yield TransactionMapper.toDto(t, ut);
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{accId}/transactions/{txId}/status")
    public TransactionResponse patchStatus(@PathVariable UUID uuid, @PathVariable UUID txId, @RequestBody @Valid PatchStatusRequest req) {
        return switch (monetaryContext.updateTransactionStatus(txId, req.status(), req.paymentDate())) {
            case Result.Success(var t) -> {
                val ut = userTransactionService.find(t.id(), uuid).orElse(null);
                yield TransactionMapper.toDto(t, ut);
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DeleteMapping("/{accId}/transactions/{txId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID txId, @Nullable @RequestParam(required = false) String mode) {
        if (monetaryContext.findTransaction(txId) instanceof Result.Success(var existing)) {
            guardClosing(existing.date());
        }
        if (monetaryContext.deleteTransaction(txId, mode) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void guardClosing(LocalDate date) {
        if (closingService.validateDate(date) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
    }

    private UserTransaction saveUserTransaction(Transaction t, UUID userId, @Nullable UUID categoryId) {
        return userTransactionService.save(t.id(), userId, categoryId);
    }

    private void saveUserTransactionForGroup(Transaction first, UUID userId, @Nullable UUID categoryId) {
        val groupId = first.groupId();
        if (groupId == null) return;
        monetaryContext.listTransactions().getOrElse(List.of()).stream()
                .filter(t -> groupId.equals(t.groupId()))
                .filter(t -> !t.id().equals(first.id()))
                .forEach(t -> userTransactionService.save(t.id(), userId, categoryId));
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
