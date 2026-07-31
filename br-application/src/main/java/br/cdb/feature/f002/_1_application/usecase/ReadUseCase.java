package br.cdb.feature.f002._1_application.usecase;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f002._1_application.service.ClosingService;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.usecase.CreditCardUseCase;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.commons.Logger;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toda a leitura da fatia {@code f002} (contas, saldos e período de fechamento) — o par de
 * {@link WriteUseCase}, mesmo arranjo CQRS de {@code f006}. Context-wired como as demais classes
 * ex-contexto ({@code Context.tryGet(ReadUseCase.class)}, nunca {@code @Inject}); os
 * {@code *Resource} da fatia leem <b>só</b> daqui.
 *
 * <p>As duas camadas convivem no mesmo tipo: métodos de <b>entrada</b> ({@link #accounts()},
 * {@link #account}, {@link #monthlyBalance}, {@link #yearBalances}, {@link #balances}) aplicam a
 * política de usuário — guarda de propriedade via {@link UserGuards}, bean CDI publicado no
 * {@code Context} por {@code f999.FeatureBootstrap} e resolvido <b>por chamada</b> ({@code @RequestScoped},
 * nunca guardado em campo) — e compõem a {@link AccountView}; métodos de <b>engine</b>
 * ({@link #listAccounts}, {@link #findAccount}), consumidos cross-slice por {@code f000.UserGuards},
 * {@code f006} (importação) e {@code f999} (dispatch SSE), não aplicam guarda: têm a
 * <b>guarda implícita</b> do {@code F002_ACCOUNT.COD_PERSON} no WHERE.
 *
 * <p>Cartão tem fatia própria ({@code f003}): esta classe só <em>lê</em> {@link CreditCard} para
 * popular {@link AccountView#cards()} (projeção somente-leitura); mutação de cartão é de {@code f003}.
 */
@NullMarked
public class ReadUseCase {

    private final AccountService service = Context.tryGet(AccountService.class);
    private final BalanceService balanceService = Context.tryGet(BalanceService.class);
    private final CreditCardUseCase ucCreditCard = Context.tryGet(CreditCardUseCase.class);
    private final ReadUseCases transactions = Context.tryGet(ReadUseCases.class);

    /** Conta (dono+cor inclusos) + cartões; {@code transactions} é a lista completa (o saldo
     *  corrente do DTO é derivado dela). */
    @NullMarked
    public record AccountView(
            Account account,
            List<CreditCard> cards,
            List<Transaction> transactions
    ) {}

    /** Bean CDI resolvido a cada chamada: {@code @RequestScoped}, nunca guardado em campo. */
    private static UserGuards guards() {
        return Context.get(UserGuards.class);
    }

    /** Registrado por {@code F002Module} com {@code Context.set} — resolvido por chamada para que a
     *  construção da classe não dependa do módulo já ter rodado (testes de unidade). */
    private static ClosingService closingService() {
        return Context.get(ClosingService.class);
    }

    // ── Contas (entrada HTTP) ──────────────────────────────────────

    public Result<List<AccountView>, BusinessError> accounts() {
        val personId = HTTPRequest.personId();
        return listAccounts(personId).map(accounts -> accounts.stream()
                .map(account -> new AccountView(
                        account,
                        cards(account.id(), personId),
                        transactionsOf(account.id())
                ))
                .toList());
    }

    public Result<AccountView, BusinessError> account(UUID accountId) {
        return guards().ownsAccount(accountId).flatMap(ignored -> {
            val personId = HTTPRequest.personId();
            val account = findAccount(accountId, personId);
            if (account.isFailure()) {
                Logger.warn("Conta %s não pertence ao usuário", accountId.toString());
                return Result.failure(new BusinessError.NotFound(accountId.toString()));
            }

            return Result.success(new AccountView(
                    account.get(),
                    cards(accountId, personId),
                    transactionsOf(accountId)
            ));
        });
    }

    // ── Contas (engine — guarda implícita por COD_PERSON) ──────────

    public Result<List<Account>, BusinessError> listAccounts(String personId) {
        return Result.success(service.findAllByPerson(personId));
    }

    public Result<Account, BusinessError> findAccount(UUID id, String personId) {
        return service.findByIdAndPerson(id, personId);
    }

    // ── Saldos ─────────────────────────────────────────────────────

    public Result<Balance, BusinessError> monthlyBalance(UUID accountId, YearMonth period) {
        return guards().ownsAccount(accountId).flatMap(ignored -> service.findById(accountId)
                .flatMap(account -> balanceService.findByAccountAndPeriod(accountId, period)));
    }

    public Result<List<Balance>, BusinessError> yearBalances(UUID accountId, int year) {
        return guards().ownsAccount(accountId).flatMap(ignored -> service.findById(accountId)
                .map(account -> balanceService.findByAccountAndYear(accountId, year)));
    }

    /** Saldo do período para todas as contas do usuário numa única leitura (evita N requisições
     *  no frontend). Contas sem snapshot no período (ex.: antes da primeira movimentação) são
     *  omitidas — o chamador decide o fallback (ex.: saldo atual da conta). */
    public Result<List<Balance>, BusinessError> balances(YearMonth period) {
        val personId = HTTPRequest.personId();
        val result = new ArrayList<Balance>();
        for (val account : listAccounts(personId).getOrElse(List.of())) {
            if (balanceService.findByAccountAndPeriod(account.id(), period) instanceof Result.Success(var balance)) {
                result.add(balance);
            }
        }
        return Result.success(result);
    }

    // ── Fechamento ─────────────────────────────────────────────────

    /** Período de fechamento vigente, ou vazio quando não há — leitura de {@code GET /accounts/closing}
     *  (também consumida por {@code f006} via {@code InternalApi}). */
    public Optional<YearMonth> closingPeriod() {
        return closingService().find();
    }

    // ── Leituras auxiliares do lado de escrita ─────────────────────

    /** Cartões da conta (projeção somente-leitura de {@code f003}); vazio quando a conta não é da pessoa. */
    public List<CreditCard> cards(UUID accountId, String personId) {
        return ucCreditCard.list(accountId, personId).getOrElse(List.of());
    }

    /** Quantas transações da pessoa estão ligadas à conta — usado pelo delete sem estratégia
     *  ({@code DeletionOutcome.Linked}). */
    public int transactionCount(UUID personId, UUID accountId) {
        return (int) transactions.transactions(personId.toString()).getOrElse(List.<Transaction>of()).stream()
                .filter(t -> accountId.equals(t.accountId()))
                .count();
    }

    private List<Transaction> transactionsOf(UUID accountId) {
        return transactions.transactions(accountId).getOrElse(List.of());
    }
}
