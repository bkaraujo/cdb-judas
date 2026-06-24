package br.community.context.monetary;

import br.commons.MessageBus;
import br.commons.Registry;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.monetary._1_application.event.TransactionEventListener;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.monetary._1_application.service.CostCenterService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Composition root do contexto monetário: monta o grafo de serviços/use-cases/listeners e publica
 * a {@link MonetaryContext} no {@link Registry}. Livre de Spring.
 */
@NullMarked
public final class MonetaryBootstrap {

    private MonetaryBootstrap() {}

    public static void register() {
        val accountService = new AccountService(Registry.get(AccountRepository.class));
        val balanceService = new BalanceService(Registry.get(BalanceRepository.class));
        val costCenterService = new CostCenterService(Registry.get(CostCenterRepository.class));
        val transactionService = new TransactionService(Registry.get(TransactionRepository.class));

        val accountUseCase = new AccountUseCase(accountService, balanceService);
        val metadataUseCase = new MetadataUseCase(costCenterService);
        val transactionUseCase = new TransactionUseCase(transactionService);

        MessageBus.subscribe(new TransactionEventListener(accountService, balanceService, transactionService));

        Registry.set(MonetaryContext.class, new MonetaryContext(accountUseCase, transactionUseCase, metadataUseCase));
    }
}
