package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.model.Account;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.MonthlyBalance;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.AccountCommand;
import br.cdb.context.monetary._1_application.command.TransactionPolicy;
import br.cdb.context.monetary._1_application.service.*;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
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
 * Cobre as operações de conta do contexto monetary. As regras de cartão (conta vinculada, limites)
 * são uma preocupação da feature ({@code USER_ACCOUNT}) e estão cobertas em {@code AccountResourceTest}.
 */
class AccountUseCaseTest {

    private InMemoryRepositories.Accounts accountRepo;
    private InMemoryRepositories.Balances balanceRepo;
    private InMemoryRepositories.Transactions txRepo;
    private InMemoryRepositories.Cards cardRepo;
    private AccountUseCase useCase;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryRepositories.Accounts();
        balanceRepo = new InMemoryRepositories.Balances();
        txRepo = new InMemoryRepositories.Transactions();
        cardRepo = new InMemoryRepositories.Cards();

        val accountService = new AccountService(accountRepo);
        val balanceService = new BalanceService(balanceRepo);
        val transactionService = new TransactionService(txRepo);
        val cardService = new CardService(cardRepo);
        val balanceRecalculationService = new BalanceRecalculationService(accountService, balanceService, transactionService);

        useCase = new AccountUseCase(accountService, balanceService, transactionService, cardService, balanceRecalculationService);
    }

    private Account seedChecking() {
        return seedChecking("Banco", true);
    }

    private Account seedChecking(String name, boolean active) {
        UUID id = UUID.randomUUID();
        Account acc = new Account(id, name, Account.Type.CHECKING, active);
        return accountRepo.save(acc);
    }

    private AccountCommand checkingCmd() {
        return new AccountCommand("Banco", "CHECKING", "#112233", true,
                null, null, null, null);
    }

    private Transaction seedTransaction(UUID accountId) {
        return txRepo.save(new Transaction(UUID.randomUUID(), "compra", BigDecimal.TEN, LocalDate.of(2026, 5, 10),
                accountId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, UUID.randomUUID(), null, null, 1, 1, null, null));
    }

    @Test
    @DisplayName("cria CHECKING normalmente")
    void createsCheckingAccount() {
        Result<Account, BusinessError> r = useCase.createAccount(checkingCmd());
        assertTrue(r.isSuccess());
        assertEquals(1, accountRepo.findAll().size());
    }

    @Test
    @DisplayName("tipo desconhecido → Validation")
    void rejectsUnknownAccountType() {
        AccountCommand cmd = new AccountCommand("X", "SAVINGS", "#112233", true,
                null, null, null, null);
        Result<Account, BusinessError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Validation.class, ((Result.Failure<Account, BusinessError>) r).error());
    }

    @Test
    @DisplayName("limite/ciclo de fatura sobrevivem a create e update, e all-null limpa")
    void limitFieldsRoundTripThroughCreateAndUpdate() {
        AccountCommand withLimit = new AccountCommand("Banco", "CHECKING", "#112233", true,
                new BigDecimal("1000.00"), new BigDecimal("200.00"), 5, 10);
        Account created = ((Result.Success<Account, BusinessError>) useCase.createAccount(withLimit)).value();
        assertEquals(0, new BigDecimal("1000.00").compareTo(created.creditLimit()));
        assertEquals(0, new BigDecimal("200.00").compareTo(created.overdraftLimit()));
        assertEquals(5, created.closingDay());
        assertEquals(10, created.dueDay());

        AccountCommand cleared = new AccountCommand("Banco", "CHECKING", "#112233", true,
                null, null, null, null);
        Account updated = ((Result.Success<Account, BusinessError>) useCase.updateAccount(created.id(), cleared)).value();
        assertNull(updated.creditLimit());
        assertNull(updated.overdraftLimit());
        assertNull(updated.closingDay());
        assertNull(updated.dueDay());
    }

    @Test
    @DisplayName("deleteAccount inexistente → NotFound")
    void deleteUnknownAccount() {
        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(UUID.randomUUID(), new TransactionPolicy.Block());
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Block sem transações remove a conta")
    void blockDeletesEmptyAccount() {
        Account checking = seedChecking();
        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(checking.id(), new TransactionPolicy.Block());
        assertTrue(r.isSuccess());
        assertTrue(accountRepo.findAll().isEmpty());
    }

    @Test
    @DisplayName("Block com transações vinculadas → Conflict, conta preservada")
    void blockRejectsAccountWithLinkedTransactions() {
        Account checking = seedChecking();
        seedTransaction(checking.id());

        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(checking.id(), new TransactionPolicy.Block());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
        assertTrue(accountRepo.findById(checking.id()).isPresent());
    }

    @Test
    @DisplayName("Move: transações e cartões migram, saldos de origem somem, conta de origem é removida")
    void moveMigratesTransactionsCardsAndDropsSourceBalances() {
        Account source = seedChecking("Origem", true);
        Account target = seedChecking("Destino", true);
        Transaction tx = seedTransaction(source.id());
        cardRepo.save(new CreditCard(UUID.randomUUID(), "1234", source.id(), true));
        balanceRepo.save(new MonthlyBalance(UUID.randomUUID(), source.id(), YearMonth.of(2026, 5), new BigDecimal("50.00")));

        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(source.id(), new TransactionPolicy.Move(target.id()));

        assertTrue(r.isSuccess());
        assertEquals(List.of(tx.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
        assertTrue(accountRepo.findById(source.id()).isEmpty(), "conta de origem removida");
        assertEquals(target.id(), txRepo.findById(tx.id()).orElseThrow().accountId(), "transação migrada");
        assertEquals(1, cardRepo.findAll().stream().filter(c -> target.id().equals(c.accountId())).count(), "cartão migrado");
        assertTrue(balanceRepo.findByAccount(source.id()).isEmpty(), "saldos da origem apagados");
    }

    @Test
    @DisplayName("Move para si mesma é rejeitado")
    void moveRejectsSelfTarget() {
        Account account = seedChecking();
        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(account.id(), new TransactionPolicy.Move(account.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para conta inexistente → NotFound")
    void moveRejectsUnknownTarget() {
        Account account = seedChecking();
        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(account.id(), new TransactionPolicy.Move(UUID.randomUUID()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para conta inativa é rejeitado")
    void moveRejectsInactiveTarget() {
        Account source = seedChecking("Origem", true);
        Account inactiveTarget = seedChecking("Destino inativo", false);
        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(source.id(), new TransactionPolicy.Move(inactiveTarget.id()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Purge apaga conta+cartões+transações, mas a perna irmã de transferência sobrevive")
    void purgeDeletesAccountCardsAndTransactionsButKeepsTransferSibling() {
        Account source = seedChecking("Origem", true);
        Account other = seedChecking("Outra", true);
        CreditCard creditCard = cardRepo.save(new CreditCard(UUID.randomUUID(), "1234", source.id(), true));
        UUID groupId = UUID.randomUUID();
        Transaction outLeg = txRepo.save(new Transaction(UUID.randomUUID(), "Transferência (saída)", new BigDecimal("50.00"),
                LocalDate.of(2026, 5, 10), source.id(), Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE,
                UUID.randomUUID(), null, groupId, 1, 2, null, null));
        Transaction inLeg = txRepo.save(new Transaction(UUID.randomUUID(), "Transferência (entrada)", new BigDecimal("50.00"),
                LocalDate.of(2026, 5, 10), other.id(), Transaction.Status.CONFIRMED, Transaction.Type.INCOME,
                UUID.randomUUID(), null, groupId, 2, 2, null, null));

        Result<List<UUID>, BusinessError> r = useCase.deleteAccount(source.id(), new TransactionPolicy.Purge());

        assertTrue(r.isSuccess());
        assertEquals(List.of(outLeg.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
        assertTrue(accountRepo.findById(source.id()).isEmpty());
        assertTrue(cardRepo.findById(creditCard.id()).isEmpty(), "cartão da conta apagada sumiu junto");
        assertTrue(txRepo.findById(outLeg.id()).isEmpty(), "perna da conta apagada sumiu");
        assertTrue(txRepo.findById(inLeg.id()).isPresent(), "perna da outra conta sobrevive");
    }
}
