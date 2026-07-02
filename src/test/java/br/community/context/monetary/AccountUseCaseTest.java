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
        return new AccountCommand("Banco", new BigDecimal("100.00"), "CHECKING", "#112233", true);
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
        AccountCommand cmd = new AccountCommand("X", new BigDecimal("100.00"), "SAVINGS", "#112233", true);
        Result<Account, DomainError> r = useCase.createAccount(cmd);
        assertTrue(r.isFailure());
        assertInstanceOf(DomainError.Validation.class, ((Result.Failure<Account, DomainError>) r).error());
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
