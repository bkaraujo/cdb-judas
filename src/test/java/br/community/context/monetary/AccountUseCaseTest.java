package br.community.context.monetary;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._1_application.command.AccountCommand;
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

/**
 * Cobre as operações de conta do contexto monetary. As regras de cartão (conta vinculada, limites)
 * são uma preocupação da feature ({@code USER_ACCOUNT}) e estão cobertas em {@code AccountResourceTest}.
 */
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
        Account acc = new Account(id, "Banco", Account.Type.CHECKING, true);
        return accountRepo.save(acc);
    }

    private AccountCommand checkingCmd() {
        return new AccountCommand("Banco", new BigDecimal("100.00"), "CHECKING", "#112233", true,
                null, null, null, null);
    }

    @Test
    @DisplayName("cria CHECKING normalmente")
    void createsCheckingAccount() {
        Result<Account, DomainError> r = useCase.createAccount(checkingCmd());
        assertTrue(r.isSuccess());
        assertEquals(1, accountRepo.findAll().size());
    }

    @Test
    @DisplayName("tipo desconhecido → Validation")
    void rejectsUnknownAccountType() {
        AccountCommand cmd = new AccountCommand("X", new BigDecimal("100.00"), "SAVINGS", "#112233", true,
                null, null, null, null);
        Result<Account, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.Validation.class, ((Result.Failure<Account, DomainError>) r).error());
    }

    @Test
    @DisplayName("limite/ciclo de fatura sobrevivem a create e update, e all-null limpa")
    void limitFieldsRoundTripThroughCreateAndUpdate() {
        AccountCommand withLimit = new AccountCommand("Banco", new BigDecimal("100.00"), "CHECKING", "#112233", true,
                new BigDecimal("1000.00"), new BigDecimal("200.00"), 5, 10);
        Account created = ((Result.Success<Account, DomainError>) useCase.createAccount(withLimit)).value();
        assertEquals(0, new BigDecimal("1000.00").compareTo(created.creditLimit()));
        assertEquals(0, new BigDecimal("200.00").compareTo(created.overdraftLimit()));
        assertEquals(5, created.closingDay());
        assertEquals(10, created.dueDay());

        AccountCommand cleared = new AccountCommand("Banco", new BigDecimal("100.00"), "CHECKING", "#112233", true,
                null, null, null, null);
        Account updated = ((Result.Success<Account, DomainError>) useCase.updateAccount(created.id(), cleared)).value();
        assertNull(updated.creditLimit());
        assertNull(updated.overdraftLimit());
        assertNull(updated.closingDay());
        assertNull(updated.dueDay());
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
}
