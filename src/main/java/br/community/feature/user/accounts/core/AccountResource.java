package br.community.feature.user.accounts.core;

import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.shared._1_application.DomainException;
import br.community.core.web.security.CurrentUser;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountResource {

    private final MonetaryContext monetaryContext;
    private final UserAccountService userAccountService;

    @GetMapping
    public List<AccountResponse> listAll(@RequestParam(required = false) @Nullable String type) {
        val result = isCardType(type) ? monetaryContext.listCreditCards() : monetaryContext.listAccounts();
        val transactions = allTransactions();
        val userId = CurrentUser.getId();
        val uaMap = userAccountService.findByUser(userId).stream()
                .collect(Collectors.toMap(UserAccount::accountId, Function.identity()));
        return switch (result) {
            case Result.Success(var accounts) ->
                    accounts.stream().map(a -> AccountResponse.from(a, uaMap.get(a.id()), transactions)).toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    private boolean isCardType(@Nullable String type) {
        if (type == null) return false;
        val t = Strings.upper(type.replace('-', '_'));
        return t.equals("CARD") || t.equals("CREDIT_CARD");
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.findAccount(id)) {
            case Result.Success(var c) -> AccountResponse.from(c, userAccountService.find(userId, c.id()), allTransactions());
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody @Valid AccountRequest req) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.createAccount(toCommand(req))) {
            case Result.Success(var c) -> {
                val ua = new UserAccount(userId, c.id(), req.balance(), req.color(), req.active());
                userAccountService.save(ua);
                yield AccountResponse.from(c, ua, allTransactions());
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @RequestBody @Valid AccountRequest req) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.updateAccount(id, toCommand(req))) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var c) -> {
                val ua = new UserAccount(userId, c.id(), req.balance(), req.color(), req.active());
                userAccountService.save(ua);
                yield AccountResponse.from(c, ua, allTransactions());
            }
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        val userId = CurrentUser.getId();
        switch (monetaryContext.deleteAccount(id)) {
            case Result.Success(var ignored) -> userAccountService.delete(userId, id);
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }

    private AccountCommand toCommand(AccountRequest req) {
        return new AccountCommand(req.name(), req.balance(), req.type(), req.color(), req.active(), req.linkedAccountId(), req.additionalInfo());
    }

    private List<Transaction> allTransactions() {
        return monetaryContext.listTransactions().getOrElse(List.of());
    }
}
