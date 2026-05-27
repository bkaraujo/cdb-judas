package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.AccountType;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._1_application.command.AccountCommand;
import br.community.context.monetary._1_application.command.CreditCardCommand;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
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

    private MonetaryAccount seedChecking(String color) {
        UUID id = UUID.randomUUID();
        MonetaryAccount acc = new MonetaryAccount(id, "Banco", AccountType.CHECKING,
                new BigDecimal("100.00"), color, true, null);
        return accountRepo.save(acc);
    }

    private AccountCommand checkingCmd() {
        return new AccountCommand("Banco", new BigDecimal("100.00"), "CHECKING", "#112233", true, null, null);
    }

    @Test
    @DisplayName("§1 cria CHECKING normalmente")
    void createsCheckingAccount() {
        Result<MonetaryAccount, DomainError> r = useCase.createAccount(checkingCmd());
        assertTrue(r.isSuccess());
        assertEquals(1, accountRepo.findAll().size());
    }

    @Test
    @DisplayName("§2.1 CREDIT_CARD via AccountCommand sem linkedAccountId falha")
    void creditCardRequiresLinkedAccount() {
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#112233", true, null, null);
        Result<MonetaryAccount, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.BusinessRule.class, ((Result.Failure<MonetaryAccount, DomainError>) r).error());
    }

    @Test
    @DisplayName("§2.1 CREDIT_CARD ligado a não-CHECKING falha")
    void creditCardLinkedToNonChecking() {
        UUID invId = UUID.randomUUID();
        accountRepo.save(new MonetaryAccount(invId, "Inv", AccountType.INVESTMENT,
                new BigDecimal("0.00"), "#aabbcc", true, null));
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#aabbcc", true, invId, null);
        Result<MonetaryAccount, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("§2.2 cor do cartão deve casar com conta vinculada")
    void creditCardColorMismatch() {
        MonetaryAccount checking = seedChecking("#aabbcc");
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#000000", true, checking.id(), null);
        Result<MonetaryAccount, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("§2.2 comparação de cor é case-insensitive")
    void creditCardColorCaseInsensitive() {
        MonetaryAccount checking = seedChecking("#aabbcc");
        AccountCommand cmd = new AccountCommand("Card", new BigDecimal("500.00"),
                "CREDIT_CARD", "#AABBCC", true, checking.id(), null);
        Result<MonetaryAccount, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("§2 createCreditCard valida conta vinculada CHECKING e cor")
    void createCreditCardViaCommand() {
        MonetaryAccount checking = seedChecking("#ff0000");
        CreditCardCommand cmd = new CreditCardCommand(checking.id(), "Card", "1234",
                new BigDecimal("1000.00"), 10, 20, "#ff0000", true);
        Result<MonetaryAccount, DomainError> r = useCase.createCreditCard(cmd);
        assertTrue(r.isSuccess());
        MonetaryAccount card = ((Result.Success<MonetaryAccount, DomainError>) r).value();
        assertNotNull(card);
        assertEquals(AccountType.CREDIT_CARD, card.type());
        assertEquals(checking.id(), card.linkedAccountId());
        assertEquals("1234", card.additionalInfo().get("last4"));
        assertEquals(20, card.additionalInfo().get("dueDay"));
        assertEquals(10, card.additionalInfo().get("closingDay"));
    }

    @Test
    @DisplayName("§2 createCreditCard com cor diferente falha")
    void createCreditCardColorMismatch() {
        MonetaryAccount checking = seedChecking("#ff0000");
        CreditCardCommand cmd = new CreditCardCommand(checking.id(), "Card", "1234",
                new BigDecimal("1000.00"), 10, 20, "#00ff00", true);
        assertTrue(useCase.createCreditCard(cmd).isFailure());
    }

    @Test
    @DisplayName("update CREDIT_CARD sem linkedAccountId falha")
    void updateCreditCardRequiresLink() {
        MonetaryAccount checking = seedChecking("#112233");
        UUID cardId = UUID.randomUUID();
        accountRepo.save(new MonetaryAccount(cardId, "Card", AccountType.CREDIT_CARD,
                new BigDecimal("500.00"), "#112233", true, checking.id()));
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
        MonetaryAccount checking = seedChecking("#112233");
        Result<Void, DomainError> r = useCase.deleteAccount(checking.id());
        assertTrue(r.isSuccess());
        assertTrue(accountRepo.findAll().isEmpty());
    }

    @Test
    @DisplayName("listCreditCardsByAccount filtra por linkedAccountId")
    void listCreditCardsByAccount() {
        MonetaryAccount checking = seedChecking("#112233");
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        accountRepo.save(new MonetaryAccount(c1, "C1", AccountType.CREDIT_CARD,
                new BigDecimal("100.00"), "#112233", true, checking.id()));
        accountRepo.save(new MonetaryAccount(c2, "C2", AccountType.CREDIT_CARD,
                new BigDecimal("100.00"), "#112233", true, UUID.randomUUID()));
        Result<java.util.List<MonetaryAccount>, DomainError> r = useCase.listCreditCardsByAccount(checking.id());
        assertTrue(r.isSuccess());
        assertEquals(1, ((Result.Success<java.util.List<MonetaryAccount>, DomainError>) r).value().size());
    }
}
