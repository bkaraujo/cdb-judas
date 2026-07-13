package br.cdb.context.monetary;

import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.context.monetary._1_application.event.TransactionEventListener;
import br.cdb.context.monetary._1_application.service.*;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CardUseCase;
import br.cdb.context.monetary._1_application.usecase.MetadataUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.commons.MessageBus;
import br.commons.Registry;
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
        val cardService = new CardService(Registry.get(CardRepository.class));
        val balanceRecalculationService = new BalanceRecalculationService(accountService, balanceService, transactionService);

        val accountUseCase = new AccountUseCase(accountService, balanceService, transactionService, cardService, balanceRecalculationService);
        val metadataUseCase = new MetadataUseCase(costCenterService);
        val transactionUseCase = new TransactionUseCase(transactionService, cardService, balanceRecalculationService);
        val cardUseCase = new CardUseCase(cardService, accountService, transactionService, balanceRecalculationService);

        MessageBus.subscribe(new TransactionEventListener(balanceRecalculationService));

        Registry.set(MonetaryContext.class, new MonetaryContext(accountUseCase, transactionUseCase, metadataUseCase, cardUseCase));
    }
}
