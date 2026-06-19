package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.monetary._1_application.command.CreditCardCommand;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.shared._0_domain.model.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Cobre §1, §2 e §10 (regras de cartão) das business-rules. */

class AccountUseCaseTest {

    private InMemoryRepositories.Accounts accountRepo;
    private AccountUseCase useCase;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryRepositories.Accounts();
        InMemoryRepositories.Balances balanceRepo = new InMemoryRepositories.Balances();
        useCase = new AccountUseCase(new AccountService(accountRepo), new BalanceService(balanceRepo));
    }

    private Account seedChecking() {
        UUID id = UUID.randomUUID();
        Account acc = new Account(id, "Banco", Account.Type.CHECKING, true, null);
        return accountRepo.save(acc);
    }

    private AccountCommand checkingCmd() {
        return new AccountCommand("Banco", new BigDecimal("100.00"), "CHECKING", "#112233", true, null, null);
    }

    @Test
    @DisplayName("§1 cria CHECKING normalmente")
    void createsCheckingAccount() {
        Result<Account, DomainError> r = useCase.createAccount(checkingCmd());
        assertTrue(r.isSuccess());
        assertEquals(1, accountRepo.findAll().size());
    }

    @Test
    @DisplayName("§2.1 CREDIT_CARD via AccountCommand sem linkedAccountId falha")
    void creditCardRequiresLinkedAccount() {
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#112233", true, null, null);
        Result<Account, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class, ((Result.Failure<Account, DomainError>) r).error());
    }

    @Test
    @DisplayName("§2.1 CREDIT_CARD ligado a não-CHECKING falha")
    void creditCardLinkedToNonChecking() {
        UUID invId = UUID.randomUUID();
        accountRepo.save(new Account(invId, "Inv", Account.Type.INVESTMENT, true, null));
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#aabbcc", true, invId, null);
        Result<Account, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("§2.2 CREDIT_CARD ligado a CHECKING (qualquer cor) tem sucesso")
    void creditCardLinkedToCheckingSucceeds() {
        Account checking = seedChecking();
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#000000", true, checking.id(), null);
        Result<Account, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("§2 createCreditCard valida conta vinculada CHECKING")
    void createCreditCardViaCommand() {
        Account checking = seedChecking();
        CreditCardCommand cmd = new CreditCardCommand(checking.id(), "Card", "1234",
                new BigDecimal("1000.00"), 10, 20, "#ff0000", true);
        Result<Account, DomainError> r = useCase.createCreditCard(cmd);
        assertTrue(r.isSuccess());
        Account card = ((Result.Success<Account, DomainError>) r).value();
        assertNotNull(card);
        assertEquals(Account.Type.CREDIT_CARD, card.type());
        assertEquals(checking.id(), card.linkedAccountId());
        assertEquals("1234", card.additionalInfo().get("last4"));
        assertEquals(20, card.additionalInfo().get("dueDay"));
        assertEquals(10, card.additionalInfo().get("closingDay"));
    }

    @Test
    @DisplayName("update CREDIT_CARD sem linkedAccountId falha")
    void updateCreditCardRequiresLink() {
        Account checking = seedChecking();
        UUID cardId = UUID.randomUUID();
        accountRepo.save(new Account(cardId, "Card", Account.Type.CREDIT_CARD, true, checking.id()));
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#112233", true, null, null);
        assertTrue(useCase.updateAccount(cardId, cmd).isFailure());
    }

    @Test
    @DisplayName("deleteAccount inexistente → NotFound")
    void deleteUnknownAccount() {
        Result<Void, DomainError> r = useCase.deleteAccount(UUID.randomUUID());
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.NotFound.class, ((Result.Failure<Void, DomainError>) r).error());
    }

    @Test
    @DisplayName("deleteAccount sucesso remove do repo")
    void deleteAccountSuccess() {
        Account checking = seedChecking();
        Result<Void, DomainError> r = useCase.deleteAccount(checking.id());
        assertTrue(r.isSuccess());
        assertTrue(accountRepo.findAll().isEmpty());
    }

    @Test
    @DisplayName("listCreditCardsByAccount filtra por linkedAccountId")
    void listCreditCardsByAccount() {
        Account checking = seedChecking();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        accountRepo.save(new Account(c1, "C1", Account.Type.CREDIT_CARD, true, checking.id()));
        accountRepo.save(new Account(c2, "C2", Account.Type.CREDIT_CARD, true, UUID.randomUUID()));
        Result<java.util.List<Account>, DomainError> r = useCase.listCreditCardsByAccount(checking.id());
        assertTrue(r.isSuccess());
        assertEquals(1, ((Result.Success<java.util.List<Account>, DomainError>) r).value().size());
    }
}
