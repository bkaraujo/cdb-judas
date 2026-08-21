package br.cdb.feature.f002;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._1_application.command.AccountCommand;
import br.cdb.feature.f002._1_application.usecase.ReadUseCase;
import br.cdb.feature.f002._1_application.usecase.WriteUseCase;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f005._0_domain.model.Nature;
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
 * Cobre a mutação de conta da fatia {@code f002} — o par de {@code ReadUseCaseTest}. Só a camada de
 * engine ({@code upsert}/{@code delete}): os métodos de entrada dependem de
 * {@code HTTPRequest.personId()} e da fila de exclusão, e quem os cobre é
 * {@code F002AccountResourceTest}.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    private static final String PERSON_ID = UUID.randomUUID().toString();
    private static final String COLOR = "#000000";

    private WriteUseCase useCase;

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(ReadUseCases.class);
        // FQN: nome simples colide com o par de f002.
        Context.remove(br.cdb.feature.f003._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f003._1_application.usecase.WriteUseCase.class);
        useCase = new WriteUseCase();
    }

    private Account seedChecking() {
        return seedChecking("Banco", true);
    }

    private Account seedChecking(String name, boolean active) {
        val id = UUID.randomUUID();
        val acc = new Account(id, name, Account.Type.CHECKING, active);
        return accountRepository().save(acc);
    }

    private AccountCommand.Create checkingCmd() {
        return new AccountCommand.Create("Banco", "CHECKING", true, null, null, null, null);
    }

    private Transaction seedTransaction(UUID accountId) {
        return transactionRepository().save(new Transaction(UUID.randomUUID(), "compra", BigDecimal.TEN, LocalDate.of(2026, 5, 10),
                accountId, Transaction.Status.CONFIRMED, Nature.EXPENSE, UUID.randomUUID(), null, null, 1, 1, null, null));
    }

    @Test
    @DisplayName("cria CHECKING normalmente")
    void createsCheckingAccount() {
        val r = useCase.upsert(checkingCmd(), PERSON_ID, COLOR);
        assertTrue(r.isSuccess());
        assertEquals(1, accountRepository().findAll().size());
    }

    @Test
    @DisplayName("tipo desconhecido → Validation")
    void rejectsUnknownAccountType() {
        val cmd = new AccountCommand.Create("X", "SAVINGS", true, null, null, null, null);
        val r = useCase.upsert(cmd, PERSON_ID, COLOR);
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Validation.class, ((Result.Failure<Account, BusinessError>) r).error());
    }

    @Test
    @DisplayName("limite/ciclo de fatura sobrevivem a create e update, e all-null limpa")
    void limitFieldsRoundTripThroughCreateAndUpdate() {
        val withLimit = new AccountCommand.Create("Banco", "CHECKING", true, new BigDecimal("1000.00"), new BigDecimal("200.00"), 5, 10);
        val created = ((Result.Success<Account, BusinessError>) useCase.upsert(withLimit, PERSON_ID, COLOR)).value();
        assertEquals(0, new BigDecimal("1000.00").compareTo(created.creditLimit()));
        assertEquals(0, new BigDecimal("200.00").compareTo(created.overdraftLimit()));
        assertEquals(5, created.closingDay());
        assertEquals(10, created.dueDay());

        val cleared = new AccountCommand.Update(created.id(), "Banco", "CHECKING", true, null, null, null, null);
        val updated = ((Result.Success<Account, BusinessError>) useCase.upsert(cleared, PERSON_ID, COLOR)).value();
        assertNull(updated.creditLimit());
        assertNull(updated.overdraftLimit());
        assertNull(updated.closingDay());
        assertNull(updated.dueDay());
    }

    @Test
    @DisplayName("deleteAccount inexistente → NotFound")
    void deleteUnknownAccount() {
        val r = useCase.delete(new AccountCommand.Delete(UUID.randomUUID(), new TransactionPolicy.Block()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Block sem transações remove a conta")
    void blockDeletesEmptyAccount() {
        val checking = seedChecking();
        val r = useCase.delete(new AccountCommand.Delete(checking.id(), new TransactionPolicy.Block()));
        assertTrue(r.isSuccess());
        assertTrue(accountRepository().findAll().isEmpty());
    }

    @Test
    @DisplayName("Block com transações vinculadas → Conflict, conta preservada")
    void blockRejectsAccountWithLinkedTransactions() {
        val checking = seedChecking();
        seedTransaction(checking.id());

        val r = useCase.delete(new AccountCommand.Delete(checking.id(), new TransactionPolicy.Block()));

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
        assertTrue(accountRepository().findById(checking.id()).isPresent());
    }

    @Test
    @DisplayName("Move: transações e cartões migram, saldos de origem somem, conta de origem é removida")
    void moveMigratesTransactionsCardsAndDropsSourceBalances() {
        val source = seedChecking("Origem", true);
        val target = seedChecking("Destino", true);
        val tx = seedTransaction(source.id());
        cardRepository().save(new CreditCard(UUID.randomUUID(), "1234", source.id(), true));
        balanceRepository().save(new Balance(source, YearMonth.of(2026, 5), new BigDecimal("50.00")));

        val r = useCase.delete(new AccountCommand.Delete(source.id(), new TransactionPolicy.Move(target.id())));

        assertTrue(r.isSuccess());
        assertEquals(List.of(tx.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
        assertTrue(accountRepository().findById(source.id()).isEmpty(), "conta de origem removida");
        assertEquals(target.id(), transactionRepository().findById(tx.id()).orElseThrow().accountId(), "transação migrada");
        assertEquals(1, cardRepository().findAll().stream().filter(c -> target.id().equals(c.accountId())).count(), "cartão migrado");
        assertTrue(balanceRepository().findByAccount(source.id()).isEmpty(), "saldos da origem apagados");
    }

    @Test
    @DisplayName("Move para si mesma é rejeitado")
    void moveRejectsSelfTarget() {
        val account = seedChecking();
        val r = useCase.delete(new AccountCommand.Delete(account.id(), new TransactionPolicy.Move(account.id())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para conta inexistente → NotFound")
    void moveRejectsUnknownTarget() {
        val account = seedChecking();
        val r = useCase.delete(new AccountCommand.Delete(account.id(), new TransactionPolicy.Move(UUID.randomUUID())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para conta inativa é rejeitado")
    void moveRejectsInactiveTarget() {
        val source = seedChecking("Origem", true);
        val inactiveTarget = seedChecking("Destino inativo", false);
        val r = useCase.delete(new AccountCommand.Delete(source.id(), new TransactionPolicy.Move(inactiveTarget.id())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Purge apaga conta+cartões+transações, mas a perna irmã de transferência sobrevive")
    void purgeDeletesAccountCardsAndTransactionsButKeepsTransferSibling() {
        val source = seedChecking("Origem", true);
        val other = seedChecking("Outra", true);
        val creditCard = cardRepository().save(new CreditCard(UUID.randomUUID(), "1234", source.id(), true));
        val groupId = UUID.randomUUID();
        val outLeg = transactionRepository().save(new Transaction(UUID.randomUUID(), "Transferência (saída)", new BigDecimal("50.00"), LocalDate.of(2026, 5, 10), source.id(), Transaction.Status.CONFIRMED, Nature.EXPENSE, UUID.randomUUID(), null, groupId, 1, 2, null, null));
        val inLeg = transactionRepository().save(new Transaction(UUID.randomUUID(), "Transferência (entrada)", new BigDecimal("50.00"), LocalDate.of(2026, 5, 10), other.id(), Transaction.Status.CONFIRMED, Nature.INCOME, UUID.randomUUID(), null, groupId, 2, 2, null, null));

        val r = useCase.delete(new AccountCommand.Delete(source.id(), new TransactionPolicy.Purge()));

        assertTrue(r.isSuccess());
        assertEquals(List.of(outLeg.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
        assertTrue(accountRepository().findById(source.id()).isEmpty());
        assertTrue(cardRepository().findById(creditCard.id()).isEmpty(), "cartão da conta apagada sumiu junto");
        assertTrue(transactionRepository().findById(outLeg.id()).isEmpty(), "perna da conta apagada sumiu");
        assertTrue(transactionRepository().findById(inLeg.id()).isPresent(), "perna da outra conta sobrevive");
    }
}
