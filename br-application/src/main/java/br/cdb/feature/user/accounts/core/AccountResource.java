package br.cdb.feature.user.accounts.core;

import br.cdb.context.monetary.MonetaryContext;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.AccountCommand;
import br.cdb.core.web.security.CurrentUser;
import br.cdb.feature.user.accounts.transactions.UserTransactionService;
import br.cdb.feature.user.deletion.DeletionStrategy;
import br.cdb.feature.user.deletion.Deletions;
import br.cdb.feature.user.tags.UserTransactionTagService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AccountResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final MonetaryContext monetaryContext;
    private final UserAccountService userAccountService;
    private final UserTransactionService userTransactionService;
    private final UserTransactionTagService tagLinkService;
    private final AccountStreamPublisher accountStreamPublisher;

    @GET
    public List<AccountResponse> listAll() {
        val transactions = allTransactions();
        val userId = CurrentUser.getId();
        val uaMap = userAccountService.findByUser(userId).stream()
                .collect(Collectors.toMap(UserAccount::accountId, Function.identity()));
        val cardsByAccount = monetaryContext.listCards().getOrElse(List.of()).stream()
                .collect(Collectors.groupingBy(CreditCard::accountId));
        return switch (monetaryContext.listAccounts()) {
            case Result.Success(var accounts) -> accounts.stream()
                    .map(account -> AccountResponse.from(account, uaMap.get(account.id()),
                            cardsByAccount.getOrDefault(account.id(), List.of()), transactions))
                    .toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @GET
    @Path("/{id}")
    public AccountResponse getById(@PathParam("id") UUID id) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.findAccount(id)) {
            case Result.Success(var c) -> AccountResponse.from(c, userAccountService.find(userId, c.id()),
                    cardsOf(c.id()), allTransactions());
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @POST
    public RestResponse<AccountResponse> create(@Valid AccountRequest req) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.createAccount(toCommand(req))) {
            case Result.Success(var account) -> {
                val ua = overlay(userId, account.id(), req);
                userAccountService.save(ua);
                accountStreamPublisher.upsert(account.id());
                yield RestResponse.status(RestResponse.Status.CREATED,
                        AccountResponse.from(account, ua, List.of(), allTransactions()));
            }
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public AccountResponse update(@PathParam("id") UUID id, @Valid AccountRequest req) {
        val userId = CurrentUser.getId();
        return switch (monetaryContext.updateAccount(id, toCommand(req))) {
            case Result.Failure(var error) -> throw new BusinessException(error);
            case Result.Success(var c) -> {
                val ua = overlay(userId, c.id(), req);
                userAccountService.save(ua);
                accountStreamPublisher.upsert(c.id());
                yield AccountResponse.from(c, ua, cardsOf(c.id()), allTransactions());
            }
        };
    }

    @DELETE
    @Path("/{id}")
    public Response delete(
            @PathParam("uuid") UUID uuid,
            @PathParam("id") UUID id,
            @QueryParam("strategy") @Nullable String strategy,
            @QueryParam("targetId") @Nullable UUID targetId
    ) {
        val parsed = Deletions.parse(strategy, targetId, ALLOWED_STRATEGIES);
        if (parsed instanceof Result.Failure<DeletionStrategy, BusinessError>(var error)) {
            throw new BusinessException(error);
        }
        val deletionStrategy = parsed.get();

        if (deletionStrategy == null) {
            val count = countLinkedTransactions(id);
            if (count > 0) {
                return Deletions.linkedConflict(null, count, "Existem " + count + " transações vinculadas a esta conta.");
            }
        }

        return executeDelete(uuid, id, deletionStrategy, targetId);
    }

    /**
     * USER_ACCOUNT/USER_TRANSACTION referenciam MON_ACCOUNT (FK) — o overlay precisa sumir/ser
     * re-keyed *antes* do contexto apagar a conta. MOVE é validado aqui primeiro (boundary da
     * feature) para nunca reatribuir para um alvo inválido; Block/Purge não têm alvo a validar e
     * contam com o contexto como backstop (race → 409/400 simples, sem corrupção de dados).
     */
    private Response executeDelete(UUID uuid, UUID id, @Nullable DeletionStrategy deletionStrategy, @Nullable UUID targetId) {
        val userId = CurrentUser.getId();

        if (deletionStrategy == DeletionStrategy.MOVE) {
            val targetCheck = validateMoveTarget(id, Objects.requireNonNull(targetId));
            if (targetCheck instanceof Result.Failure<Void, BusinessError>(var error)) throw new BusinessException(error);
            userTransactionService.reassignAccount(id, targetId, uuid);
        } else {
            userTransactionService.deleteByAccountAndUser(id, uuid);
        }
        userAccountService.delete(userId, id);

        val policy = Deletions.toPolicy(deletionStrategy, targetId);
        return switch (monetaryContext.deleteAccount(id, policy)) {
            case Result.Failure(var error) -> throw new BusinessException(error);
            case Result.Success(var ids) -> {
                if (deletionStrategy != DeletionStrategy.MOVE) {
                    ids.forEach(tagLinkService::deleteByTransaction);
                }
                accountStreamPublisher.delete(id);
                if (deletionStrategy == DeletionStrategy.MOVE) {
                    accountStreamPublisher.upsert(Objects.requireNonNull(targetId));
                }
                yield Response.noContent().build();
            }
        };
    }

    private Result<Void, BusinessError> validateMoveTarget(UUID sourceId, UUID targetId) {
        return monetaryContext.findAccount(targetId).flatMap(target -> {
            if (target.id().equals(sourceId)) {
                return Result.failure(new BusinessError.BusinessRule("Target account must be different from source: " + targetId));
            }
            if (!target.active()) {
                return Result.failure(new BusinessError.BusinessRule("Target account is inactive: " + targetId));
            }
            return Result.success();
        });
    }

    private int countLinkedTransactions(UUID accountId) {
        return (int) allTransactions().stream().filter(t -> accountId.equals(t.accountId())).count();
    }

    private List<CreditCard> cardsOf(UUID accountId) {
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
