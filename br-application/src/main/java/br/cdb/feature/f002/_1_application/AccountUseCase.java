package br.cdb.feature.f002._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.Balance;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.AccountCommand;
import br.cdb.context.monetary._1_application.command.CreditCardCommand;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.core.web.Request;
import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f000._1_application.UserGuards;
import br.cdb.feature.f002._0_domain.UserAccount;
import br.cdb.feature.f005._0_domain.event.TransactionsDeleted;
import br.cdb.feature.f005._1_application.UserTransactionService;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
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
 * Use case da fatia {@code f002} (accounts, com cards e balance — fatias finas demais para
 * hexágono próprio, fundidas aqui per .claude/refactor.md). Nome coincide com o use case do
 * contexto monetário; referenciado por FQN completo (campo {@code ucAccount}) para evitar
 * colisão de import.
 *
 * <p>{@code deleteAccount} ainda chama {@link UserTransactionService#reassignAccount}/
 * {@link UserTransactionService#deleteByAccountAndPerson} diretamente <em>antes</em> do delete no
 * contexto — {@code PERSON_TRANSACTION} referencia {@code MON_ACCOUNT} via FK, então o overlay
 * precisa sumir/ser re-keyed antes que o contexto possa apagar a conta (sem {@code ON DELETE
 * CASCADE} ainda, essa chamada não pode virar reação a evento pós-delete). Já a limpeza de
 * vínculo de tag <em>depois</em> do delete (transações puramente apagadas, sem FK pendente)
 * publica {@link TransactionsDeleted} — mesmo padrão de {@code TagUseCase}/{@code CategoryUseCase}.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class AccountUseCase {

    private final br.cdb.context.monetary._1_application.usecase.AccountUseCase ucAccount =
            MonetaryUseCases.ucAccount();
    private final CreditCardUseCase ucCreditCard = MonetaryUseCases.ucCreditCard();
    private final TransactionUseCase ucTransaction = MonetaryUseCases.ucTransaction();

    private final UserGuards guards;
    private final UserAccountService userAccountService;
    private final UserTransactionService userTransactionService;
    private final AccountStreamPublisher accountStreamPublisher;

    /** Conta do contexto + overlay do usuário + cartões; {@code transactions} é a lista completa
     *  (o saldo corrente do DTO é derivado dela). */
    @NullMarked
    public record AccountView(
            Account account,
            @Nullable UserAccount overlay,
            List<CreditCard> cards,
            List<Transaction> transactions
    ) {}

    // ── Accounts ───────────────────────────────────────────────────

    public Result<List<AccountView>, BusinessError> accounts() {
        val personId = Request.personId();

        val views = new ArrayList<AccountView>();
        for (val overlay : userAccountService.findByPerson(personId)) {
            val account = ucAccount.findAccount(overlay.accountId());
            if (account.isFailure()) {
                Logger.warn("overlay %s aponta para conta inexistente %s", overlay.personId(), overlay.accountId());
                continue;
            }

            views.add(new AccountView(
                    account.get(),
                    overlay,
                    ucCreditCard.list(overlay.accountId()).getOrElse(List.of()),
                    transactionsOf(overlay.accountId())
            ));
        }

        return Result.success(views);
    }

    public Result<AccountView, BusinessError> account(UUID accountId) {
        return guards.ownsAccount(accountId).flatMap(ignored -> {
            val overlay = userAccountService.find(Request.personId(), accountId);

            val account = ucAccount.findAccount(accountId);
            if (account.isFailure()) {
                Logger.warn("Conta %s não pertence ao usuário", accountId.toString());
                return Result.failure(new BusinessError.NotFound(accountId.toString()));
            }

            return Result.success(new AccountView(
                    account.get(),
                    overlay,
                    ucCreditCard.list(accountId).getOrElse(List.of()),
                    transactionsOf(accountId)
            ));
        });
    }

    public Result<AccountView, BusinessError> createAccount(AccountCommand.Create cmd, String color) {
        val personId = Request.personId();
        return ucAccount.upsert(cmd).map(account -> {
            val overlay = new UserAccount(personId, account.id(), color);
            userAccountService.save(overlay);
            accountStreamPublisher.upsert(account.id());
            return new AccountView(account, overlay, List.of(), List.of());
        });
    }

    public Result<AccountView, BusinessError> updateAccount(AccountCommand.Update cmd, String color) {
        return guards.ownsAccount(cmd.id()).flatMap(ignored -> {
            val personId = Request.personId();
            return ucAccount.upsert(cmd).map(account -> {
                val overlay = new UserAccount(personId, account.id(), color);
                userAccountService.save(overlay);
                accountStreamPublisher.upsert(account.id());
                return new AccountView(account, overlay, cardsOf(account.id()), List.of());
            });
        });
    }

    /**
     * PERSON_ACCOUNT/PERSON_TRANSACTION referenciam MON_ACCOUNT (FK) — o overlay precisa sumir/ser
     * re-keyed <em>antes</em> do contexto apagar a conta. MOVE é validado aqui primeiro (fronteira
     * da feature) para nunca reatribuir para um alvo inválido; Block/Purge não têm alvo a validar e
     * contam com o contexto como backstop (race → 409/400 simples, sem corrupção de dados).
     */
    public Result<DeletionOutcome, BusinessError> deleteAccount(
            UUID personId, UUID id, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        val guard = strategy == DeletionStrategy.MOVE
                ? guards.ownsAccounts(id, Objects.requireNonNull(targetId))
                : guards.ownsAccount(id);
        if (guard instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);

        if (strategy == null) {
            val count = (int) allTransactions().stream().filter(t -> id.equals(t.accountId())).count();
            if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
        }

        if (strategy == DeletionStrategy.MOVE) {
            val target = Objects.requireNonNull(targetId);
            val targetCheck = validateAccountMoveTarget(id, target);
            if (targetCheck instanceof Result.Failure<Void, BusinessError>(var error)) return Result.failure(error);
            userTransactionService.reassignAccount(id, target, personId);
        } else {
            userTransactionService.deleteByAccountAndPerson(id, personId);
        }
        userAccountService.delete(Request.personId(), id);

        return ucAccount.delete(new AccountCommand.Delete(id, Deletions.toPolicy(strategy, targetId))).map(ids -> {
            if (strategy != DeletionStrategy.MOVE) {
                MessageBus.submit(new TransactionsDeleted(ids));
            }
            accountStreamPublisher.delete(id);
            if (strategy == DeletionStrategy.MOVE) {
                accountStreamPublisher.upsert(Objects.requireNonNull(targetId));
            }
            return new DeletionOutcome.Completed();
        });
    }

    private Result<Void, BusinessError> validateAccountMoveTarget(UUID sourceId, UUID targetId) {
        return ucAccount.findAccount(targetId).flatMap(target -> {
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
        val personId = Request.personId();
        val result = new ArrayList<Balance>();
        for (val overlay : userAccountService.findByPerson(personId)) {
            if (ucAccount.getMonthlyBalance(overlay.accountId(), period) instanceof Result.Success(var balance)) {
                result.add(balance);
            }
        }
        return Result.success(result);
    }

    // ── Credit cards ───────────────────────────────────────────────

    public Result<List<CreditCard>, BusinessError> cards(UUID accountId) {
        return guards.ownsAccount(accountId).flatMap(ignored -> ucCreditCard.list(accountId));
    }

    public Result<CreditCard, BusinessError> createCard(CreditCardCommand.Create cmd) {
        return guards.ownsAccount(cmd.accountId()).flatMap(ignored -> ucCreditCard.upsert(cmd))
                .ifSuccess(ignored -> accountStreamPublisher.upsert(cmd.accountId()));
    }

    public Result<DeletionOutcome, BusinessError> deleteCard(
            UUID accountId, UUID cardId, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return guards.ownsCard(accountId, cardId).flatMap(ignored -> {
            if (strategy == null) {
                val count = (int) allTransactions().stream().filter(t -> cardId.equals(t.cardId())).count();
                if (count > 0) return Result.success(new DeletionOutcome.Linked(count));
            }

            return ucCreditCard.delete(new CreditCardCommand.Delete(cardId, Deletions.toPolicy(strategy, targetId))).map(ids -> {
                // MOVE mantém o cartão de destino na mesma conta: sem re-key de overlay a fazer.
                if (strategy != DeletionStrategy.MOVE) {
                    MessageBus.submit(new TransactionsDeleted(ids));
                }
                accountStreamPublisher.upsert(accountId);
                return new DeletionOutcome.Completed();
            });
        });
    }

    public Result<CreditCard, BusinessError> setCardActive(UUID accountId, UUID cardId, boolean active) {
        return guards.ownsCard(accountId, cardId)
                .flatMap(ignored -> ucCreditCard.upsert(new CreditCardCommand.Update(cardId, active)))
                .ifSuccess(ignored -> accountStreamPublisher.upsert(accountId));
    }

    // ── Helpers ────────────────────────────────────────────────────

    private List<CreditCard> cardsOf(UUID accountId) {
        return ucCreditCard.list(accountId).getOrElse(List.of());
    }

    private List<Transaction> transactionsOf(UUID accountId) {
        return ucTransaction.transactions(accountId).getOrElse(List.of());
    }

    private List<Transaction> allTransactions() {
        return ucTransaction.transactions().getOrElse(List.of());
    }
}
