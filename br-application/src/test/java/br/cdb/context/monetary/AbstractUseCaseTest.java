package br.cdb.context.monetary;

import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f004._1_application.service.TagService;
import br.cdb.feature.f004._1_application.service.TransactionTagService;
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
    protected TagRepository tagRepository() { return Context.get(TagRepository.class); }
    protected TransactionTagRepository transactionTagRepository() { return Context.get(TransactionTagRepository.class); }

    @BeforeEach
    public void beforeEach() {
        MessageBus.reset();
        Context.remove(CreditCardService.class);
        Context.remove(AccountService.class);
        Context.remove(BalanceService.class);
        Context.remove(CostCenterService.class);
        Context.remove(TransactionService.class);
        Context.remove(TagService.class);
        Context.remove(TransactionTagService.class);

        Context.tryGet(CreditCardRepository.class, InMemoryRepositories.Cards::new).clearCache();
        Context.tryGet(BalanceRepository.class, InMemoryRepositories.Balances::new).clearCache();
        Context.tryGet(AccountRepository.class, InMemoryRepositories.Accounts::new).clearCache();
        Context.tryGet(TransactionRepository.class, InMemoryRepositories.Transactions::new).clearCache();
        Context.tryGet(CostCenterRepository.class, InMemoryRepositories.CostCenters::new).clearCache();
        // TagRepository/TransactionTagRepository não estendem Repository (não têm clearCache): em vez
        // de limpar o fake, publica-se um novo a cada teste — o que também sobrescreve o adaptador
        // JDBC deixado por um @QuarkusTest anterior.
        Context.set(TagRepository.class, InMemoryRepositories.Tags::new);
        Context.set(TransactionTagRepository.class, InMemoryRepositories.TransactionTags::new);

        // CostCenterUseCase resolve o service com Context.get() estrito (em produção quem registra é
        // F000Module): re-registra sobre os fakes acima, depois de removido.
        Context.set(CostCenterService.class, CostCenterService::new);
    }

}


