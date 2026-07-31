package br.cdb.feature.f003._1_application.usecase;

import br.cdb.context.monetary.AbstractUseCaseTest;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f003._0_domain.model.CreditCard;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura de cartão da fatia {@code f003} — o par de {@code WriteUseCaseTest}. A entrada
 * ({@code cards}) depende de {@code HTTPRequest.personId()}, então só a guarda é exercitada aqui
 * (dublada em {@link #guards}); o caminho HTTP completo é de {@code F003CardResourceTest}. Os fakes
 * in-memory não modelam {@code COD_PERSON}, então o {@code personId} das buscas de engine não filtra
 * — quem cobre a guarda implícita é {@code F000UserGuardsIdorTest}.
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
                        : Result.failure(new BusinessError.NotFound("Account not found: " + accountId));
            }
        });
    }

    private Account seedAccount() {
        return accountRepository().save(new Account(UUID.randomUUID(), "Banco", Account.Type.CHECKING, true));
    }

    private CreditCard seedCard(UUID accountId, String last4) {
        return cardRepository().save(new CreditCard(UUID.randomUUID(), last4, accountId, true));
    }

    private void seedTransaction(UUID accountId, UUID cardId) {
        transactionRepository().save(new Transaction(UUID.randomUUID(), "compra", BigDecimal.TEN, LocalDate.of(2026, 5, 10),
                accountId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, UUID.randomUUID(), null, null, 1, 1, null, cardId));
    }

    @Test
    @DisplayName("list(personId) devolve os cartões da pessoa")
    void listsCardsOfPerson() {
        val account = seedAccount();
        seedCard(account.id(), "1234");
        seedCard(account.id(), "5678");

        val r = reads.list(PERSON_ID);

        assertTrue(r.isSuccess());
        assertEquals(2, ((Result.Success<List<CreditCard>, BusinessError>) r).value().size());
    }

    @Test
    @DisplayName("list(accountId, personId) devolve os cartões da conta")
    void listsCardsOfAccount() {
        val account = seedAccount();
        val card = seedCard(account.id(), "1234");

        val r = reads.list(account.id(), PERSON_ID);

        assertTrue(r.isSuccess());
        assertEquals(List.of(card.id()),
                ((Result.Success<List<CreditCard>, BusinessError>) r).value().stream().map(CreditCard::id).toList());
    }

    @Test
    @DisplayName("list de conta inexistente → NotFound")
    void listRejectsUnknownAccount() {
        val r = reads.list(UUID.randomUUID(), PERSON_ID);
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<CreditCard>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("cards de conta de outro dono → NotFound (guarda aplicada antes da leitura)")
    void cardsIsGuarded() {
        val account = seedAccount();
        seedCard(account.id(), "1234");
        guards(false);

        val r = new ReadUseCase().cards(account.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<List<CreditCard>, BusinessError>) r).error());
    }

    @Test
    @DisplayName("transactionCount conta só as transações do cartão pedido")
    void transactionCountIsScopedToCard() {
        val account = seedAccount();
        val card = seedCard(account.id(), "1234");
        val other = seedCard(account.id(), "5678");
        seedTransaction(account.id(), card.id());
        seedTransaction(account.id(), card.id());
        seedTransaction(account.id(), other.id());
        seedTransaction(account.id(), null);

        assertEquals(2, reads.transactionCount(PERSON_ID, card.id()));
    }
}
