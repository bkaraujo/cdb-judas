package br.cdb.feature.f002;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._1_application.usecase.ReadUseCase;
import br.cdb.feature.f006._0_domain.model.Status;
import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura da fatia {@code f002} — o par de {@code WriteUseCaseTest} (que ficou só com a
 * escrita). Os métodos de entrada que dependem de {@code HTTPRequest.personId()}
 * ({@code accounts()}/{@code balances()}) não são exercitáveis fora de uma requisição: quem os cobre
 * é {@code F002AccountResourceTest}. A guarda de propriedade é dublada aqui ({@link #guards}) —
 * o que se testa é que ela é <em>aplicada</em>, não a sua lógica interna (isso é
 * {@code F000UserGuardsIdorTest}).
 */
class ReadUseCaseTest extends AbstractUseCaseTest {

    private static final String PERSON_ID = UUID.randomUUID().toString();

    private ReadUseCase reads;

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(ReadUseCases.class);
        // FQN: nome simples colide com o par de f002.
        Context.remove(br.cdb.feature.f003._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f003._1_application.usecase.WriteUseCase.class);
        guards(true);
        reads = new ReadUseCase();
    }

    /** Dublê de {@link UserGuards}: bean {@code @RequestScoped} resolvido por chamada pelo use case. */
    private static void guards(boolean owns) {
        Context.set(UserGuards.class, () -> new UserGuards() {
            @Override
            public Result<Void, BusinessError> ownsAccount(UUID accountId) {
                return owns
                        ? Result.success()
                        : Result.failure(new BusinessError.NotFound("Account not found: %s", accountId));
            }
        });
    }

    private Account seedAccount(String name) {
        return accountRepository().save(new Account(UUID.randomUUID(), name, Account.Type.CHECKING, true));
    }

    private void seedBalance(Account account, YearMonth period, String value) {
        balanceRepository().save(new Balance(account, period, new BigDecimal(value)));
    }

    private void seedTransaction(UUID accountId) {
        transactionRepository().save(new Transaction(UUID.randomUUID(), "compra", BigDecimal.TEN, LocalDate.of(2026, 5, 10),
                accountId, Status.CONFIRMED, false, false, null, null, 1, 1, null, null));
    }

    @Test
    @DisplayName("listAccounts devolve as contas da pessoa")
    void listsAccountsOfPerson() {
        seedAccount("Banco");
        seedAccount("Corretora");

        val r = reads.listAccounts(PERSON_ID);

        assertTrue(r.isSuccess());
        assertEquals(2, ((Result.Success<List<Account>, BusinessError>) r).value().size());
    }

    @Test
    @DisplayName("findAccount de conta inexistente → NotFound")
    void findUnknownAccount() {
        val r = reads.findAccount(UUID.randomUUID(), PERSON_ID);
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Account, BusinessError>) r).error());
    }

    @Test
    @DisplayName("findAccount devolve a conta existente")
    void findExistingAccount() {
        val account = seedAccount("Banco");
        val r = reads.findAccount(account.id(), PERSON_ID);
        assertTrue(r.isSuccess());
        assertEquals(account.id(), ((Result.Success<Account, BusinessError>) r).value().id());
    }

    @Test
    @DisplayName("monthlyBalance devolve o snapshot do período")
    void monthlyBalanceReturnsPeriodSnapshot() {
        val account = seedAccount("Banco");
        seedBalance(account, YearMonth.of(2026, 4), "10.00");
        seedBalance(account, YearMonth.of(2026, 5), "50.00");

        val r = reads.monthlyBalance(account.id(), YearMonth.of(2026, 5));

        assertTrue(r.isSuccess());
        assertEquals(0, new BigDecimal("50.00").compareTo(((Result.Success<Balance, BusinessError>) r).value().value()));
    }

    @Test
    @DisplayName("monthlyBalance sem snapshot no período → NotFound")
    void monthlyBalanceWithoutSnapshot() {
        val account = seedAccount("Banco");
        val r = reads.monthlyBalance(account.id(), YearMonth.of(2026, 5));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Balance, BusinessError>) r).error());
    }

    @Test
    @DisplayName("monthlyBalance de conta de outro dono → NotFound (guarda aplicada antes da leitura)")
    void monthlyBalanceIsGuarded() {
        val account = seedAccount("Banco");
        seedBalance(account, YearMonth.of(2026, 5), "50.00");
        guards(false);

        val r = new ReadUseCase().monthlyBalance(account.id(), YearMonth.of(2026, 5));

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Balance, BusinessError>) r).error());
    }

    @Test
    @DisplayName("yearBalances devolve só o ano pedido, ordenado por período")
    void yearBalancesFiltersAndSorts() {
        val account = seedAccount("Banco");
        seedBalance(account, YearMonth.of(2026, 5), "50.00");
        seedBalance(account, YearMonth.of(2026, 1), "10.00");
        seedBalance(account, YearMonth.of(2025, 12), "5.00");

        val r = reads.yearBalances(account.id(), 2026);

        assertTrue(r.isSuccess());
        val balances = ((Result.Success<List<Balance>, BusinessError>) r).value();
        assertEquals(List.of(YearMonth.of(2026, 1), YearMonth.of(2026, 5)), balances.stream().map(Balance::period).toList());
    }

    @Test
    @DisplayName("yearBalances de conta de outro dono → NotFound")
    void yearBalancesIsGuarded() {
        val account = seedAccount("Banco");
        guards(false);

        val r = new ReadUseCase().yearBalances(account.id(), 2026);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<Balance>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("transactionCount conta só as transações da conta pedida")
    void transactionCountIsScopedToAccount() {
        val account = seedAccount("Banco");
        val other = seedAccount("Corretora");
        seedTransaction(account.id());
        seedTransaction(account.id());
        seedTransaction(other.id());

        assertEquals(2, reads.transactionCount(UUID.randomUUID(), account.id()));
    }
}
