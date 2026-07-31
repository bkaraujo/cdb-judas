package br.cdb.feature.f002._1_application;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._0_domain.event.AccountDeleted;
import br.cdb.feature.f000._0_domain.event.AccountStreamEvents;
import br.cdb.feature.f000._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.DeletionQueue;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._1_application.command.AccountCommand;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case da fatia {@code f002} (accounts, com balance — fatia fina demais para hexágono próprio,
 * fundida aqui per .claude/refactor.md). Cartão tem fatia própria (f003): esta classe só lê
 * {@code CreditCard} do contexto para popular {@link AccountView#cards()} (projeção
 * somente-leitura); mutação de cartão é {@code f003.CardUseCase}. Nome coincide com o use case do
 * contexto monetário; referenciado por FQN completo (campo {@code ucAccount}) para evitar
 * colisão de import.
 *
 * <p>{@code deleteAccount} não reatribui nada imperativamente no MOVE: reassign de conta em
 * transação é feito pela própria engine ({@code AccountUseCase.deleteMove}, contexto monetário) ao
 * mover as transações. A conta é uma linha única (dono+cor inclusos) apagada pela própria engine
 * antes do evento pós-delete ({@link AccountDeleted}/{@link TransactionsDeleted}) ser publicado —
 * sem FK entre tabelas de dados, o contexto pode apagar a raiz primeiro; o evento só resta pros
 * listeners de outras fatias (overlay de tags/categoria das transações apagadas). Cada evento
 * também vira uma linha em {@code F999_DELETION_QUEUE} (via {@link DeletionQueue}) — rede de
 * segurança pro job de f999 reprocessar se o listener best-effort falhar ou o processo cair no meio
 * da cascata. No MOVE, a conta de destino é marcada suja ({@link BalanceService#markDirty}) — o
 * próprio job recomputa.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class AccountUseCase {

    private final br.cdb.feature.f002._1_application.usecase.AccountUseCase ucAccount =
            Context.tryGet(br.cdb.feature.f002._1_application.usecase.AccountUseCase.class);
    private final CreditCardUseCase ucCreditCard = Context.tryGet(CreditCardUseCase.class);
    private final ReadUseCases reads = Context.tryGet(ReadUseCases.class);

    private final UserGuards guards;
    private final DeletionQueue deletionQueue;
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);

    /** Conta (dono+cor inclusos) + cartões; {@code transactions} é a lista completa (o saldo
     *  corrente do DTO é derivado dela). */
    @NullMarked
    public record AccountView(
            Account account,
            List<CreditCard> cards,
            List<Transaction> transactions
    ) {}

    // ── Accounts ───────────────────────────────────────────────────

    public Result<List<AccountView>, BusinessError> accounts() {
        val personId = HTTPRequest.personId();
        return ucAccount.listAccounts(personId).map(accounts -> accounts.stream()
                .map(account -> new AccountView(
                        account,
                        ucCreditCard.list(account.id(), personId).getOrElse(List.of()),
                        transactionsOf(account.id())
                ))
                .toList());
    }

    public Result<AccountView, BusinessError> account(UUID accountId) {
        return guards.ownsAccount(accountId).flatMap(ignored -> {
            val personId = HTTPRequest.personId();
            val account = ucAccount.findAccount(accountId, personId);
            if (account.isFailure()) {
                Logger.warn("Conta %s não pertence ao usuário", accountId.toString());
                return Result.failure(new BusinessError.NotFound(accountId.toString()));
            }

            return Result.success(new AccountView(
                    account.get(),
                    ucCreditCard.list(accountId, personId).getOrElse(List.of()),
                    transactionsOf(accountId)
            ));
        });
    }

    public Result<AccountView, BusinessError> createAccount(AccountCommand.Create cmd, String color) {
        val personId = HTTPRequest.personId();
        return ucAccount.upsert(cmd, personId, color).map(account -> {
            MessageBus.submit(new AccountStreamEvents.Created(account.id(), personId));
            return new AccountView(account, List.of(), List.of());
        });
    }

    public Result<AccountView, BusinessError> updateAccount(AccountCommand.Update cmd, String color) {
        return guards.ownsAccount(cmd.id()).flatMap(ignored -> {
            val personId = HTTPRequest.personId();
            return ucAccount.upsert(cmd, personId, color).map(account -> {
                MessageBus.submit(new AccountStreamEvents.Updated(account.id(), personId));
                return new AccountView(account, cardsOf(account.id(), personId), List.of());
            });
        });
    }

    /**
     * MOVE tem o alvo validado aqui primeiro (fronteira da feature) para nunca reatribuir transações
     * para uma conta inválida — a engine (contexto monetário) faz o re-key de fato ao apagar.
     * Sem FK forçando ordem: o contexto apaga a raiz (linha única, dono+cor inclusos) e só então
     * publica-se {@link AccountDeleted} (todas as estratégias) e, fora do MOVE,
     * {@link TransactionsDeleted} (overlay/tag das transações apagadas).
     */
    public Result<DeletionOutcome, BusinessError> deleteAccount(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        val guard = strategy == DeletionStrategy.MOVE
                ? guards.ownsAccounts(id, Objects.requireNonNull(targetId))
                : guards.ownsAccount(id);
        if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);

        if (strategy == null) {
            val count = (int) allTransactions(personId).stream().filter(t -> id.equals(t.accountId())).count();
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
        }

        if (strategy == DeletionStrategy.MOVE) {
            val target = Objects.requireNonNull(targetId);
            val targetCheck = validateAccountMoveTarget(id, target, personId);
            if (targetCheck instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
        }

        return ucAccount.delete(new AccountCommand.Delete(id, Deletions.toPolicy(strategy, targetId))).map(ids -> {
            MessageBus.submit(new AccountDeleted(id, personId));
            deletionQueue.enqueueAccountDeleted(id, personId);

            if (strategy != DeletionStrategy.MOVE) {
                MessageBus.submit(new TransactionsDeleted(ids));
                ids.forEach(txId -> deletionQueue.enqueueTransactionDeleted(txId, personId));
            }

            MessageBus.submit(new AccountStreamEvents.Deleted(id, personId.toString()));
            if (strategy == DeletionStrategy.MOVE) {
                val target = Objects.requireNonNull(targetId);
                balanceService.markDirty(target);
                MessageBus.submit(new AccountStreamEvents.Refresh(target, personId.toString()));
            }
            return new DeletionOutcome.Completed();
        });
    }

    private Result<Void, BusinessError> validateAccountMoveTarget(UUID sourceId, UUID targetId, UUID personId) {
        return ucAccount.findAccount(targetId, personId.toString()).flatMap(target -> {
            if (target.id().equals(sourceId)) {
                return Result.failure(new BusinessError.BusinessRule("Target account must be different from source: " + targetId));
            }
            if (!target.active()) {
                return Result.failure(new BusinessError.BusinessRule("Target account is inactive: " + targetId));
            }
            return Result.success();
        });
    }

    // ── Balances ───────────────────────────────────────────────────

    public Result<Balance, BusinessError> monthlyBalance(UUID accountId, YearMonth period) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucAccount.getMonthlyBalance(accountId, period));
    }

    public Result<List<Balance>, BusinessError> yearBalances(UUID accountId, int year) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucAccount.getYearBalances(accountId, year));
    }

    /** Saldo do período para todas as contas do usuário numa única leitura (evita N requisições
     *  no frontend). Contas sem snapshot no período (ex.: antes da primeira movimentação) são
     *  omitidas — o chamador decide o fallback (ex.: saldo atual da conta). */
    public Result<List<Balance>, BusinessError> balances(YearMonth period) {
        val personId = HTTPRequest.personId();
        val result = new ArrayList<Balance>();
        for (val account : ucAccount.listAccounts(personId).getOrElse(List.of())) {
            if (ucAccount.getMonthlyBalance(account.id(), period) instanceof Result.Success(var balance)) {
                result.add(balance);
            }
        }
        return Result.success(result);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private List<CreditCard> cardsOf(UUID accountId, String personId) {
        return ucCreditCard.list(accountId, personId).getOrElse(List.of());
    }

    private List<Transaction> transactionsOf(UUID accountId) {
        return reads.transactions(accountId).getOrElse(List.of());
    }

    private List<Transaction> allTransactions(UUID personId) {
        return reads.transactions(personId.toString()).getOrElse(List.of());
    }
}
