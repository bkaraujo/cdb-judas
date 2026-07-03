package br.community.feature.user.accounts.core;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.shared._1_application.DomainException;
import br.community.core.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AccountResource {

    private final MonetaryContext monetaryContext;
    private final UserAccountService userAccountService;

    @GET
    public List<AccountResponse> listAll() {
        val transactions = allTransactions();
        val userId = CurrentUser.getId();
        val uaMap = userAccountService.findByUser(userId).stream()
                .collect(Collectors.toMap(UserAccount::accountId, Function.identity()));
        val cardsByAccount = monetaryContext.listCards().getOrElse(List.of()).stream()
                .collect(Collectors.groupingBy(Card::accountId));
        return switch (monetaryContext.listAccounts()) {
            case Result.Success(var accounts) -> accounts.stream()
                    .map(account -> AccountResponse.from(account, uaMap.get(account.id()),
                            cardsByAccount.getOrDefault(account.id(), List.of()), transactions))
                    .toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @GET
    @Path("/{id}")
    public AccountResponse getById(@PathParam("id") UUID id) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.findAccount(id)) {
            case Result.Success(var c) -> AccountResponse.from(c, userAccountService.find(userId, c.id()),
                    cardsOf(c.id()), allTransactions());
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @POST
    public RestResponse<AccountResponse> create(@Valid AccountRequest req) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.createAccount(toCommand(req))) {
            case Result.Success(var account) -> {
                val ua = overlay(userId, account.id(), req);
                userAccountService.save(ua);
                yield RestResponse.status(RestResponse.Status.CREATED,
                        AccountResponse.from(account, ua, List.of(), allTransactions()));
            }
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public AccountResponse update(@PathParam("id") UUID id, @Valid AccountRequest req) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.updateAccount(id, toCommand(req))) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var c) -> {
                val ua = overlay(userId, c.id(), req);
                userAccountService.save(ua);
                yield AccountResponse.from(c, ua, cardsOf(c.id()), allTransactions());
            }
        };
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        val userId = CurrentUser.getId();
        switch (monetaryContext.deleteAccount(id)) {
            case Result.Success(var ignored) -> userAccountService.delete(userId, id);
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }

    private List<Card> cardsOf(UUID accountId) {
        return monetaryContext.listCardsByAccount(accountId).getOrElse(List.of());
    }

    private AccountCommand toCommand(AccountRequest req) {
        return new AccountCommand(req.name(), req.type(), req.color(), req.active(),
                req.creditLimit(), req.overdraftLimit(), req.closingDay(), req.dueDay());
    }

    private UserAccount overlay(String userId, UUID accountId, AccountRequest req) {
        return new UserAccount(userId, accountId, req.color());
    }

    private List<Transaction> allTransactions() {
        return monetaryContext.listTransactions().getOrElse(List.of());
    }
}
