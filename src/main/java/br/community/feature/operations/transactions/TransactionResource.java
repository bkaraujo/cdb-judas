package br.community.feature.operations.transactions;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionResource {

    private final MonetaryContext monetaryContext;

    @GetMapping
    public List<Transaction> listAll(@Nullable @RequestParam(required = false) Integer limit) {
        return switch (monetaryContext.listTransactions()) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var all) -> {
                val transactions = (limit != null && limit > 0 && limit < all.size())
                        ? all.subList(0, limit)
                        : all;
                yield transactions.stream().map(this::toDto).toList();
            }
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@RequestBody @Valid TransactionRequest req) {
        return switch (monetaryContext.createTransaction(toCommand(req))) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction transfer(@RequestBody @Valid TransferRequest req) {
        return switch (monetaryContext.createTransfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public Transaction update(@PathVariable UUID id, @RequestBody @Valid TransactionRequest req) {
        return switch (monetaryContext.updateTransaction(id, toCommand(req))) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}/status")
    public Transaction patchStatus(@PathVariable UUID id, @RequestBody @Valid PatchStatusRequest req) {
        return switch (monetaryContext.updateTransactionStatus(id, req.status(), req.paymentDate())) {
            case Result.Success(var t) -> toDto(t);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @Nullable @RequestParam(required = false) String mode) {
        val result = monetaryContext.deleteTransaction(id, mode);
        if (result instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
    }

    private Transaction toDto(br.community.context.monetary._0_domain.model.MonetaryTransaction t) {
        return new Transaction(t.id(), t.description(), t.amount(), t.date(),
                t.categoryId(), t.accountId(), t.status(), t.type(),
                t.paymentDate(), t.groupId(), t.installmentNumber(), t.totalInstallments());
    }

    private TransactionCommand toCommand(TransactionRequest req) {
        return new TransactionCommand(req.description(), req.amount(), req.date(),
                req.categoryId(), req.accountId(), req.status(), req.type(),
                req.installments(), req.editMode());
    }
}
