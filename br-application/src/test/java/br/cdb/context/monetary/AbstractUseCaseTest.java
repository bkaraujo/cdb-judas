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
import br.commons.Registry;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;

@NullMarked
public abstract class AbstractUseCaseTest {

    protected CreditCardRepository cardRepository() { return Registry.get(CreditCardRepository.class); }
    protected BalanceRepository balanceRepository() { return Registry.get(BalanceRepository.class); }
    protected AccountRepository accountRepository() { return Registry.get(AccountRepository.class); }
    protected CostCenterRepository costCenterRepository() { return Registry.get(CostCenterRepository.class); }
    protected TransactionRepository transactionRepository() { return Registry.get(TransactionRepository.class); }

    @BeforeEach
    public void beforeEach() {
        MessageBus.reset();
        Registry.remove(CreditCardService.class);
        Registry.remove(AccountService.class);
        Registry.remove(BalanceService.class);
        Registry.remove(CostCenterService.class);
        Registry.remove(TransactionService.class);

        Registry.tryGet(CreditCardRepository.class, InMemoryRepositories.Cards::new).clearCache();
        Registry.tryGet(BalanceRepository.class, InMemoryRepositories.Balances::new).clearCache();
        Registry.tryGet(AccountRepository.class, InMemoryRepositories.Accounts::new).clearCache();
        Registry.tryGet(TransactionRepository.class, InMemoryRepositories.Transactions::new).clearCache();
        Registry.tryGet(CostCenterRepository.class, InMemoryRepositories.CostCenters::new).clearCache();
    }

}


