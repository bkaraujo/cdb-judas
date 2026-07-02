package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.AccountLimit;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._1_application.command.AccountLimitCommand;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.monetary._1_application.event.TransactionEventListener;
import br.community.context.monetary._1_application.service.AccountLimitService;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.monetary._1_application.service.CardService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.monetary._1_application.usecase.CardUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Cobre cartão (identificado só pelo last4, vinculado a conta real) e limite/ciclo de fatura da conta. */
class CardUseCaseTest {

    private InMemoryRepositories.Accounts accountRepo;
    private InMemoryRepositories.Cards cardRepo;
    private InMemoryRepositories.AccountLimits accountLimitRepo;
    private CardUseCase useCase;
    private TransactionEventListener listener;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryRepositories.Accounts();
        cardRepo = new InMemoryRepositories.Cards();
        accountLimitRepo = new InMemoryRepositories.AccountLimits();
        val cardService = new CardService(cardRepo);
        val accountLimitService = new AccountLimitService(accountLimitRepo);
        useCase = new CardUseCase(cardService, new AccountService(accountRepo), accountLimitService);
        listener = new TransactionEventListener(
                new AccountService(accountRepo), new BalanceService(new InMemoryRepositories.Balances()),
                new TransactionService(new InMemoryRepositories.Transactions()), cardService, accountLimitService);
    }

    private Account seedChecking() {
        Account acc = new Account(UUID.randomUUID(), "Banco", Account.Type.CHECKING, true);
        return accountRepo.save(acc);
    }

    private Account seedInactive() {
        Account acc = new Account(UUID.randomUUID(), "Banco Inativo", Account.Type.CHECKING, false);
        return accountRepo.save(acc);
    }

    @Test
    @DisplayName("cria cartão vinculado a conta existente e ativa")
    void createsCard() {
        Account account = seedChecking();
        Result<Card, DomainError> r = useCase.createCard(new CardCommand(account.id(), "1234"));
        assertTrue(r.isSuccess());
        Card card = ((Result.Success<Card, DomainError>) r).value();
        assertEquals("1234", card.last4());
        assertEquals(account.id(), card.accountId());
        assertTrue(card.active());
    }

    @Test
    @DisplayName("last4 fora do formato \\d{4} → Validation")
    void rejectsMalformedLast4() {
        Account account = seedChecking();
        Result<Card, DomainError> r = useCase.createCard(new CardCommand(account.id(), "12a4"));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.Validation.class, ((Result.Failure<Card, DomainError>) r).error());
    }

    @Test
    @DisplayName("conta inexistente → NotFound")
    void rejectsUnknownAccount() {
        Result<Card, DomainError> r = useCase.createCard(new CardCommand(UUID.randomUUID(), "1234"));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<Card, DomainError>) r).error());
    }

    @Test
    @DisplayName("conta inativa → BusinessRule")
    void rejectsInactiveAccount() {
        Account inactive = seedInactive();
        Result<Card, DomainError> r = useCase.createCard(new CardCommand(inactive.id(), "1234"));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class, ((Result.Failure<Card, DomainError>) r).error());
    }

    @Test
    @DisplayName("last4 duplicado na mesma conta → Conflict")
    void rejectsDuplicateLast4OnSameAccount() {
        Account account = seedChecking();
        assertTrue(useCase.createCard(new CardCommand(account.id(), "1234")).isSuccess());
        Result<Card, DomainError> r = useCase.createCard(new CardCommand(account.id(), "1234"));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.Conflict.class, ((Result.Failure<Card, DomainError>) r).error());
    }

    @Test
    @DisplayName("mesmo last4 em contas diferentes é permitido")
    void allowsSameLast4OnDifferentAccounts() {
        Account a = seedChecking();
        Account b = seedChecking();
        assertTrue(useCase.createCard(new CardCommand(a.id(), "1234")).isSuccess());
        assertTrue(useCase.createCard(new CardCommand(b.id(), "1234")).isSuccess());
    }

    @Test
    @DisplayName("deleteCard remove o cartão")
    void deletesCard() {
        Account account = seedChecking();
        Card card = ((Result.Success<Card, DomainError>) useCase.createCard(new CardCommand(account.id(), "1234"))).value();
        Result<Void, DomainError> r = useCase.deleteCard(card.id());
        assertTrue(r.isSuccess());
        assertTrue(((Result.Success<List<Card>, DomainError>) useCase.listCardsByAccount(account.id())).value().isEmpty());
    }

    @Test
    @DisplayName("deleteCard inexistente → NotFound")
    void deleteUnknownCard() {
        Result<Void, DomainError> r = useCase.deleteCard(UUID.randomUUID());
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<Void, DomainError>) r).error());
    }

    @Test
    @DisplayName("getAccountLimit de conta sem linha devolve limites nulos")
    void getAccountLimitDefaultsToNulls() {
        Account account = seedChecking();
        Result<AccountLimit, DomainError> r = useCase.getAccountLimit(account.id());
        assertTrue(r.isSuccess());
        AccountLimit limit = ((Result.Success<AccountLimit, DomainError>) r).value();
        assertNull(limit.creditLimit());
        assertNull(limit.overdraftLimit());
        assertNull(limit.closingDay());
        assertNull(limit.dueDay());
    }

    @Test
    @DisplayName("setAccountLimit cria, atualiza e apaga (upsert all-null)")
    void setAccountLimitUpsertsAndDeletesWhenEmpty() {
        Account account = seedChecking();

        Result<AccountLimit, DomainError> created = useCase.setAccountLimit(account.id(),
                new AccountLimitCommand(new BigDecimal("1000.00"), new BigDecimal("200.00"), 5, 10));
        assertTrue(created.isSuccess());
        AccountLimit createdLimit = ((Result.Success<AccountLimit, DomainError>) created).value();
        assertEquals(0, new BigDecimal("1000.00").compareTo(createdLimit.creditLimit()));
        assertEquals(5, createdLimit.closingDay());

        Result<AccountLimit, DomainError> updated = useCase.setAccountLimit(account.id(),
                new AccountLimitCommand(new BigDecimal("2000.00"), null, 6, 12));
        AccountLimit updatedLimit = ((Result.Success<AccountLimit, DomainError>) updated).value();
        assertEquals(0, new BigDecimal("2000.00").compareTo(updatedLimit.creditLimit()));
        assertNull(updatedLimit.overdraftLimit());
        assertEquals(6, updatedLimit.closingDay());

        Result<AccountLimit, DomainError> cleared = useCase.setAccountLimit(account.id(),
                new AccountLimitCommand(null, null, null, null));
        AccountLimit clearedLimit = ((Result.Success<AccountLimit, DomainError>) cleared).value();
        assertNull(clearedLimit.creditLimit());
        assertNull(clearedLimit.closingDay());

        AccountLimit reread = ((Result.Success<AccountLimit, DomainError>) useCase.getAccountLimit(account.id())).value();
        assertNull(reread.creditLimit());
    }

    @Test
    @DisplayName("TransactionEventListener.onAccountDeleted remove cartões e limite da conta")
    void accountDeletionCascadesCardsAndLimit() {
        Account account = seedChecking();
        Card card = ((Result.Success<Card, DomainError>) useCase.createCard(new CardCommand(account.id(), "1234"))).value();
        useCase.setAccountLimit(account.id(), new AccountLimitCommand(new BigDecimal("500.00"), null, null, null));

        listener.onAccountDeleted(new AccountEvents.Deleted(account.id()));

        assertTrue(cardRepo.findById(card.id()).isEmpty(), "cartão removido junto da conta");
        assertTrue(accountLimitRepo.findById(account.id()).isEmpty(), "limite removido junto da conta");
    }
}
