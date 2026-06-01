package br.community.feature.user.accounts;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountResource {

    private final MonetaryContext monetaryContext;

    @GetMapping
    public List<Account> listAll(@RequestParam(required = false) @Nullable String type) {
        val result = isCardType(type) ? monetaryContext.listCreditCards() : monetaryContext.listAccounts();
        val transactions = allTransactions();
        return switch (result) {
            case Result.Success(var accounts) -> accounts.stream().map(a -> Account.from(a, transactions)).toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    private boolean isCardType(@Nullable String type) {
        if (type == null) return false;
        val t = type.replace('-', '_').toUpperCase();
        return t.equals("CARD") || t.equals("CREDIT_CARD");
    }

    @GetMapping("/{id}/balance")
    public Object getBalance(
            @PathVariable UUID id,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer year
    ) {
        if (period != null) {
            val ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyyMM"));
            return switch (monetaryContext.getMonthlyBalance(id, ym)) {
                case Result.Success(var b) -> b;
                case Result.Failure(var error) -> throw new DomainException(error);
            };
        }
        if (year != null) {
            return switch (monetaryContext.getYearBalances(id, year)) {
                case Result.Success(var balances) -> balances;
                case Result.Failure(var error) -> throw new DomainException(error);
            };
        }
        throw new DomainException(new DomainError.Validation("'period' or 'year' must be provided"));
    }

    @GetMapping("/{id}")
    public Account getById(@PathVariable UUID id) {
        return switch (monetaryContext.findAccount(id)) {
            case Result.Success(var c) -> Account.from(c, allTransactions());
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@RequestBody @Valid AccountRequest req) {
        return switch (monetaryContext.createAccount(toCommand(req))) {
            case Result.Success(var c) -> Account.from(c, allTransactions());
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public Account update(@PathVariable UUID id, @RequestBody @Valid AccountRequest req) {
        return switch (monetaryContext.updateAccount(id, toCommand(req))) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var c) -> Account.from(c, allTransactions());
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        switch (monetaryContext.deleteAccount(id)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }

    private AccountCommand toCommand(AccountRequest req) {
        return new AccountCommand(req.name(), req.balance(), req.type(), req.color(), req.active(), req.linkedAccountId(), req.additionalInfo());
    }

    private List<MonetaryTransaction> allTransactions() {
        return monetaryContext.listTransactions().getOrElse(List.of());
    }
}
