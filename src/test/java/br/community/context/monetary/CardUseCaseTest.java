package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.monetary._1_application.command.TransactionPolicy;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceRecalculationService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.monetary._1_application.service.CardService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.monetary._1_application.usecase.CardUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Cobre cartão: identificado só pelo last4, sempre vinculado a uma conta real existente e ativa. */
class CardUseCaseTest {

    private InMemoryRepositories.Accounts accountRepo;
    private InMemoryRepositories.Cards cardRepo;
    private InMemoryRepositories.Transactions txRepo;
    private CardUseCase useCase;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryRepositories.Accounts();
        cardRepo = new InMemoryRepositories.Cards();
        txRepo = new InMemoryRepositories.Transactions();

        val accountService = new AccountService(accountRepo);
        val cardService = new CardService(cardRepo);
        transactionService = new TransactionService(txRepo);
        val balanceRecalculationService = new BalanceRecalculationService(
                accountService, new BalanceService(new InMemoryRepositories.Balances()), transactionService);

        useCase = new CardUseCase(cardService, accountService, transactionService, balanceRecalculationService);
    }

    private Account seedChecking() {
        Account acc = new Account(UUID.randomUUID(), "Banco", Account.Type.CHECKING, true);
        return accountRepo.save(acc);
    }

    private Account seedInactive() {
        Account acc = new Account(UUID.randomUUID(), "Banco Inativo", Account.Type.CHECKING, false);
        return accountRepo.save(acc);
    }

    private Card createCard(UUID accountId, String last4) {
        return ((Result.Success<Card, DomainError>) useCase.createCard(new CardCommand(accountId, last4))).value();
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
    @DisplayName("Block sem transações remove o cartão")
    void blockDeletesCardWithoutTransactions() {
        Account account = seedChecking();
        Card card = createCard(account.id(), "1234");
        Result<List<UUID>, DomainError> r = useCase.deleteCard(card.id(), new TransactionPolicy.Block());
        assertTrue(r.isSuccess());
        assertTrue(((Result.Success<List<Card>, DomainError>) useCase.listCardsByAccount(account.id())).value().isEmpty());
    }

    @Test
    @DisplayName("deleteCard inexistente → NotFound")
    void deleteUnknownCard() {
        Result<List<UUID>, DomainError> r = useCase.deleteCard(UUID.randomUUID(), new TransactionPolicy.Block());
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<List<UUID>, DomainError>) r).error());
    }

    @Test
    @DisplayName("Block com transação vinculada → Conflict, cartão preservado")
    void blockRejectsCardWithLinkedTransaction() {
        Account account = seedChecking();
        Card card = createCard(account.id(), "1234");
        transactionService.save(new Transaction(UUID.randomUUID(), "compra", java.math.BigDecimal.TEN, java.time.LocalDate.now(),
                account.id(), Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, UUID.randomUUID(), null, null, 1, 1, null, card.id()));

        Result<List<UUID>, DomainError> r = useCase.deleteCard(card.id(), new TransactionPolicy.Block());

        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.Conflict.class, ((Result.Failure<List<UUID>, DomainError>) r).error());
        assertTrue(cardRepo.findById(card.id()).isPresent());
    }

    @Test
    @DisplayName("Move: transações migram para o cartão de destino, origem é removida")
    void moveMigratesTransactionsToTargetCard() {
        Account account = seedChecking();
        Card source = createCard(account.id(), "1234");
        Card target = createCard(account.id(), "5678");
        Transaction tx = transactionService.save(new Transaction(UUID.randomUUID(), "compra", java.math.BigDecimal.TEN,
                java.time.LocalDate.now(), account.id(), Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE,
                UUID.randomUUID(), null, null, 1, 1, null, source.id()));

        Result<List<UUID>, DomainError> r = useCase.deleteCard(source.id(), new TransactionPolicy.Move(target.id()));

        assertTrue(r.isSuccess());
        assertEquals(List.of(tx.id()), ((Result.Success<List<UUID>, DomainError>) r).value());
        assertTrue(cardRepo.findById(source.id()).isEmpty());
        assertEquals(target.id(), txRepo.findById(tx.id()).orElseThrow().cardId());
    }

    @Test
    @DisplayName("Move para si mesmo é rejeitado")
    void moveRejectsSelfTarget() {
        Account account = seedChecking();
        Card card = createCard(account.id(), "1234");
        Result<List<UUID>, DomainError> r = useCase.deleteCard(card.id(), new TransactionPolicy.Move(card.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class, ((Result.Failure<List<UUID>, DomainError>) r).error());
    }

    @Test
    @DisplayName("Move para cartão inexistente → NotFound")
    void moveRejectsUnknownTarget() {
        Account account = seedChecking();
        Card card = createCard(account.id(), "1234");
        Result<List<UUID>, DomainError> r = useCase.deleteCard(card.id(), new TransactionPolicy.Move(UUID.randomUUID()));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<List<UUID>, DomainError>) r).error());
    }

    @Test
    @DisplayName("Move para cartão de outra conta é rejeitado")
    void moveRejectsTargetFromAnotherAccount() {
        Account a = seedChecking();
        Account b = seedChecking();
        Card source = createCard(a.id(), "1234");
        Card target = createCard(b.id(), "5678");
        Result<List<UUID>, DomainError> r = useCase.deleteCard(source.id(), new TransactionPolicy.Move(target.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class, ((Result.Failure<List<UUID>, DomainError>) r).error());
    }

    @Test
    @DisplayName("Move para cartão inativo é rejeitado")
    void moveRejectsInactiveTarget() {
        Account account = seedChecking();
        Card source = createCard(account.id(), "1234");
        Card target = createCard(account.id(), "5678");
        useCase.setActive(target.id(), false);

        Result<List<UUID>, DomainError> r = useCase.deleteCard(source.id(), new TransactionPolicy.Move(target.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class, ((Result.Failure<List<UUID>, DomainError>) r).error());
    }

    @Test
    @DisplayName("Purge apaga o cartão e suas transações")
    void purgeDeletesCardAndItsTransactions() {
        Account account = seedChecking();
        Card card = createCard(account.id(), "1234");
        Transaction tx = transactionService.save(new Transaction(UUID.randomUUID(), "compra", java.math.BigDecimal.TEN,
                java.time.LocalDate.now(), account.id(), Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE,
                UUID.randomUUID(), null, null, 1, 1, null, card.id()));

        Result<List<UUID>, DomainError> r = useCase.deleteCard(card.id(), new TransactionPolicy.Purge());

        assertTrue(r.isSuccess());
        assertEquals(List.of(tx.id()), ((Result.Success<List<UUID>, DomainError>) r).value());
        assertTrue(cardRepo.findById(card.id()).isEmpty());
        assertTrue(txRepo.findById(tx.id()).isEmpty());
    }

    @Test
    @DisplayName("setActive persiste a flag")
    void setActivePersistsFlag() {
        Account account = seedChecking();
        Card card = createCard(account.id(), "1234");

        Result<Card, DomainError> r = useCase.setActive(card.id(), false);

        assertTrue(r.isSuccess());
        assertFalse(((Result.Success<Card, DomainError>) r).value().active());
        assertFalse(cardRepo.findById(card.id()).orElseThrow().active());
    }
}
