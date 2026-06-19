package br.community.context.monetary;

import br.commons.MessageBus;
import br.commons.Registry;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.monetary._1_application.event.AccountEventListener;
import br.community.context.monetary._1_application.event.TransactionEventListener;
import br.community.context.monetary._1_application.service.*;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Composition root do contexto monetário no modelo jimmy: monta o grafo de
 * serviços/use-cases/listeners e publica a {@link MonetaryContext} no {@link Registry}.
 *
 * <p>Substitui o antigo {@code MonetaryModule} (Spring {@code @Configuration}/{@code @Bean}):
 * o contexto passa a ser ligado por {@code Registry} (DI manual), sem qualquer dependência
 * de framework. As classes de domínio/aplicação seguem com injeção por construtor — apenas o
 * <em>wiring</em> mudou de Spring para {@code Registry}. A borda Spring (feature) obtém a
 * facade via {@code br.community.core.ContextBridge}.</p>
 *
 * <p>Pré-condição: os repositórios (portas) já devem estar registrados no {@code Registry}
 * antes de {@link #register()} (feito pelo {@code ContextBridge} a partir dos beans de
 * persistência). A migração JSON→JDBC por agregado (ver plano, Stage 3) troca apenas a
 * implementação registrada, sem tocar neste grafo.</p>
 */
@NullMarked
public final class MonetaryBootstrap {

    private MonetaryBootstrap() {}

    public static void register() {
        val accountService = new AccountService(Registry.get(AccountRepository.class));
        val balanceService = new BalanceService(Registry.get(BalanceRepository.class));
        val categoryService = new CategoryService(Registry.get(CategoryRepository.class));
        val costCenterService = new CostCenterService(Registry.get(CostCenterRepository.class));
        val tagService = new TagService(Registry.get(TagRepository.class));
        val transactionService = new TransactionService(Registry.get(TransactionRepository.class));

        val accountUseCase = new AccountUseCase(accountService, balanceService);
        val metadataUseCase = new MetadataUseCase(tagService, categoryService, costCenterService, transactionService);
        val transactionUseCase = new TransactionUseCase(transactionService, categoryService);

        MessageBus.subscribe(new AccountEventListener(accountService));
        MessageBus.subscribe(new TransactionEventListener(accountService, balanceService, transactionService));

        Registry.set(MonetaryContext.class, new MonetaryContext(accountUseCase, transactionUseCase, metadataUseCase));
    }
}
