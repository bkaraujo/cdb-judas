package br.cdb.context.monetary;

import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.commons.MessageBus;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;

@NullMarked
public abstract class AbstractUseCaseTest {

    protected CreditCardRepository cardRepository() { return Context.get(CreditCardRepository.class); }
    protected BalanceRepository balanceRepository() { return Context.get(BalanceRepository.class); }
    protected AccountRepository accountRepository() { return Context.get(AccountRepository.class); }
    protected CostCenterRepository costCenterRepository() { return Context.get(CostCenterRepository.class); }
    protected TransactionRepository transactionRepository() { return Context.get(TransactionRepository.class); }

    @BeforeEach
    public void beforeEach() {
        MessageBus.reset();
        Context.remove(CreditCardService.class);
        Context.remove(AccountService.class);
        Context.remove(BalanceService.class);
        Context.remove(CostCenterService.class);
        Context.remove(TransactionService.class);

        Context.tryGet(CreditCardRepository.class, InMemoryRepositories.Cards::new).clearCache();
        Context.tryGet(BalanceRepository.class, InMemoryRepositories.Balances::new).clearCache();
        Context.tryGet(AccountRepository.class, InMemoryRepositories.Accounts::new).clearCache();
        Context.tryGet(TransactionRepository.class, InMemoryRepositories.Transactions::new).clearCache();
        Context.tryGet(CostCenterRepository.class, InMemoryRepositories.CostCenters::new).clearCache();

        // CostCenterUseCase resolve o service com Context.get() estrito (em produção quem registra é
        // F000Module): re-registra sobre os fakes acima, depois de removido.
        Context.set(CostCenterService.class, CostCenterService::new);
    }

}


