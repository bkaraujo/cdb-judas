package br.community.feature.records.accounts;

import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
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
    public List<Account> listAll() {
        return switch (monetaryContext.listAccounts()) {
            case Result.Success(var accounts) -> accounts.stream().map(this::toDto).toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
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
            case Result.Success(var c) -> toDto(c);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@RequestBody @Valid AccountRequest req) {
        return switch (monetaryContext.createAccount(toCommand(req))) {
            case Result.Success(var c) -> toDto(c);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public Account update(@PathVariable UUID id, @RequestBody @Valid AccountRequest req) {
        return switch (monetaryContext.updateAccount(id, toCommand(req))) {
            case Result.Success(var c) -> toDto(c);
            case Result.Failure(var error) -> throw new DomainException(error);
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

    private Account toDto(MonetaryAccount monetary) {
        return new Account(
                monetary.id(),
                monetary.name(),
                monetary.balance(),
                Strings.upper(monetary.type().name()),
                monetary.color(),
                monetary.active(),
                monetary.linkedAccountId(),
                monetary.additionalInfo()
        );
    }
}
