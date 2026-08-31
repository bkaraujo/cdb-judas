package br.cdb.feature.f003;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f000._0_domain.TransactionPolicy;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._1_application.usecase.CreditCardCommand;
import br.cdb.feature.f003._1_application.usecase.ReadUseCase;
import br.cdb.feature.f003._1_application.usecase.WriteUseCase;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a mutação de cartão da fatia {@code f003} — o par de {@code ReadUseCaseTest}. Só a camada de
 * engine ({@code upsert}/{@code delete}): cartão é identificado só pelo last4, sempre vinculado a uma
 * conta real existente e ativa. Os métodos de entrada dependem de {@code HTTPRequest.personId()} e
 * quem os cobre é {@code F003CardResourceTest}.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    private static final String PERSON_ID = UUID.randomUUID().toString();
    private WriteUseCase useCase;

    @BeforeEach
    void setUp() {
        // Grafo Context-wired: sem isso, os singletons ficariam presos aos fakes da classe de teste
        // anterior (os services já são removidos por AbstractUseCaseTest).
        Context.remove(ReadUseCase.class);
        Context.remove(ReadUseCases.class);
        useCase = new WriteUseCase();
        populateCacheFor(UUID.fromString(PERSON_ID));
    }

    private Account seedChecking() {
        val acc = new Account(UUID.randomUUID(), "Banco", Account.Type.CHECKING, true, PERSON_ID, null, null, null, null, null, null, null);
        return accountRepository().save(acc);
    }

    private Account seedInactive() {
        val acc = new Account(UUID.randomUUID(), "Banco Inativo", Account.Type.CHECKING, false, PERSON_ID, null, null, null, null, null, null, null);
        return accountRepository().save(acc);
    }

    private CreditCard createCard(UUID accountId, String last4) {
        return ((Result.Success<CreditCard, BusinessError>) useCase.upsert(new CreditCardCommand.Create(accountId, last4))).value();
    }

    @Test
    @DisplayName("cria cartão vinculado a conta existente e ativa")
    void createsCard() {
        val account = seedChecking();
        val r = useCase.upsert(new CreditCardCommand.Create(account.id(), "1234"));

        assertTrue(r.isSuccess());
        val creditCard = ((Result.Success<CreditCard, BusinessError>) r).value();
        assertEquals("1234", creditCard.last4());
        assertEquals(account.id(), creditCard.accountId());
        assertTrue(creditCard.active());
    }

    @Test
    @DisplayName("last4 fora do formato \\d{4} → Validation")
    void rejectsMalformedLast4() {
        val account = seedChecking();
        val r = useCase.upsert(new CreditCardCommand.Create(account.id(), "12a4"));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Validation.class, ((Result.Failure<CreditCard, BusinessError>) r).error());
    }

    @Test
    @DisplayName("conta inexistente → NotFound")
    void rejectsUnknownAccount() {
        val r = useCase.upsert(new CreditCardCommand.Create(UUID.randomUUID(), "1234"));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<CreditCard, BusinessError>) r).error());
    }

    @Test
    @DisplayName("conta inativa → BusinessRule")
    void rejectsInactiveAccount() {
        val inactive = seedInactive();
        val r = useCase.upsert(new CreditCardCommand.Create(inactive.id(), "1234"));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<CreditCard, BusinessError>) r).error());
    }

    @Test
    @DisplayName("last4 duplicado na mesma conta → Conflict")
    void rejectsDuplicateLast4OnSameAccount() {
        val account = seedChecking();
        assertTrue(useCase.upsert(new CreditCardCommand.Create(account.id(), "1234")).isSuccess());
        val r = useCase.upsert(new CreditCardCommand.Create(account.id(), "1234"));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<CreditCard, BusinessError>) r).error());
    }

    @Test
    @DisplayName("mesmo last4 em contas diferentes é permitido")
    void allowsSameLast4OnDifferentAccounts() {
        val a = seedChecking();
        val b = seedChecking();
        assertTrue(useCase.upsert(new CreditCardCommand.Create(a.id(), "1234")).isSuccess());
        assertTrue(useCase.upsert(new CreditCardCommand.Create(b.id(), "1234")).isSuccess());
    }

    @Test
    @DisplayName("Block sem transações remove o cartão")
    void blockDeletesCardWithoutTransactions() {
        val account = seedChecking();
        val creditCard = createCard(account.id(), "1234");
        val r = useCase.delete(new CreditCardCommand.Delete(creditCard.id(), new TransactionPolicy.Block()));
        assertTrue(r.isSuccess());
        assertTrue(cardRepository().findById(creditCard.id()).isEmpty());
    }

    @Test
    @DisplayName("deleteCard inexistente → NotFound")
    void deleteUnknownCard() {
        val r = useCase.delete(new CreditCardCommand.Delete(UUID.randomUUID(), new TransactionPolicy.Block()));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Block com transação vinculada → Conflict, cartão preservado")
    void blockRejectsCardWithLinkedTransaction() {
        val account = seedChecking();
        val creditCard = createCard(account.id(), "1234");
        transactionRepository().save(new Transaction(UUID.randomUUID(), "compra", java.math.BigDecimal.TEN, java.time.LocalDate.now(),
                account.id(), Status.CONFIRMED, false, false, null, null, 1, 1, null, creditCard.id()));

        val r = useCase.delete(new CreditCardCommand.Delete(creditCard.id(), new TransactionPolicy.Block()));

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
        assertTrue(cardRepository().findById(creditCard.id()).isPresent());
    }

    @Test
    @DisplayName("Move: transações migram para o cartão de destino, origem é removida")
    void moveMigratesTransactionsToTargetCard() {
        val account = seedChecking();
        val source = createCard(account.id(), "1234");
        val target = createCard(account.id(), "5678");
        val tx = transactionRepository().save(new Transaction(UUID.randomUUID(), "compra", java.math.BigDecimal.TEN,
                java.time.LocalDate.now(), account.id(), Status.CONFIRMED, false,
                false, null, null, 1, 1, null, source.id()));

        val r = useCase.delete(new CreditCardCommand.Delete(source.id(), new TransactionPolicy.Move(target.id())));

        assertTrue(r.isSuccess());
        assertEquals(List.of(tx.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
        assertTrue(cardRepository().findById(source.id()).isEmpty());
        assertEquals(target.id(), transactionRepository().findById(tx.id()).orElseThrow().cardId());
    }

    @Test
    @DisplayName("Move para si mesmo é rejeitado")
    void moveRejectsSelfTarget() {
        val account = seedChecking();
        val creditCard = createCard(account.id(), "1234");
        val r = useCase.delete(new CreditCardCommand.Delete(creditCard.id(), new TransactionPolicy.Move(creditCard.id())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para cartão inexistente → NotFound")
    void moveRejectsUnknownTarget() {
        val account = seedChecking();
        val creditCard = createCard(account.id(), "1234");
        val r = useCase.delete(new CreditCardCommand.Delete(creditCard.id(), new TransactionPolicy.Move(UUID.randomUUID())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para cartão de outra conta é rejeitado")
    void moveRejectsTargetFromAnotherAccount() {
        val a = seedChecking();
        val b = seedChecking();
        val source = createCard(a.id(), "1234");
        val target = createCard(b.id(), "5678");
        val r = useCase.delete(new CreditCardCommand.Delete(source.id(), new TransactionPolicy.Move(target.id())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Move para cartão inativo é rejeitado")
    void moveRejectsInactiveTarget() {
        val account = seedChecking();
        val source = createCard(account.id(), "1234");
        val target = createCard(account.id(), "5678");
        useCase.upsert(new CreditCardCommand.Update(target.id(), false));

        val r = useCase.delete(new CreditCardCommand.Delete(source.id(), new TransactionPolicy.Move(target.id())));
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.BusinessRule.class, ((Result.Failure<List<UUID>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("Purge apaga o cartão e suas transações")
    void purgeDeletesCardAndItsTransactions() {
        val account = seedChecking();
        val creditCard = createCard(account.id(), "1234");
        val tx = transactionRepository().save(new Transaction(UUID.randomUUID(), "compra", java.math.BigDecimal.TEN,
                java.time.LocalDate.now(), account.id(), Status.CONFIRMED, false,
                false, null, null, 1, 1, null, creditCard.id()));

        val r = useCase.delete(new CreditCardCommand.Delete(creditCard.id(), new TransactionPolicy.Purge()));

        assertTrue(r.isSuccess());
        assertEquals(List.of(tx.id()), ((Result.Success<List<UUID>, BusinessError>) r).value());
        assertTrue(cardRepository().findById(creditCard.id()).isEmpty());
        assertTrue(transactionRepository().findById(tx.id()).isEmpty());
    }

    @Test
    @DisplayName("setActive persiste a flag")
    void setActivePersistsFlag() {
        val account = seedChecking();
        val creditCard = createCard(account.id(), "1234");

        val r = useCase.upsert(new CreditCardCommand.Update(creditCard.id(), false));

        assertTrue(r.isSuccess());
        assertFalse(((Result.Success<CreditCard, BusinessError>) r).value().active());
        assertFalse(cardRepository().findById(creditCard.id()).orElseThrow().active());
    }
}
