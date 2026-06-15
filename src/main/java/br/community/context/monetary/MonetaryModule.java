package br.community.context.monetary;

import br.commons.MessageBus;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.monetary._1_application.event.AccountEventListener;
import br.community.context.monetary._1_application.event.TransactionEventListener;
import br.community.context.monetary._1_application.service.*;
import br.community.context.monetary._1_application.usecase.AccountUseCase;
import br.community.context.monetary._1_application.usecase.MetadataUseCase;
import br.community.context.monetary._1_application.usecase.TransactionUseCase;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class MonetaryModule {

    // ── Services ──────────────────────────────────────────────────

    @Bean
    AccountService accountService(AccountRepository accountRepository) {
        return new AccountService(accountRepository);
    }

    @Bean
    BalanceService balanceService(BalanceRepository balanceRepository) {
        return new BalanceService(balanceRepository);
    }

    @Bean
    CategoryService categoryService(CategoryRepository categoryRepository) {
        return new CategoryService(categoryRepository);
    }

    @Bean
    CostCenterService costCenterService(CostCenterRepository costCenterRepository) {
        return new CostCenterService(costCenterRepository);
    }

    @Bean
    TagService tagService(TagRepository tagRepository) {
        return new TagService(tagRepository);
    }

    @Bean
    TransactionService transactionService(TransactionRepository transactionRepository) {
        return new TransactionService(transactionRepository);
    }

    @Bean
    ClosingService closingService(ClosingRepository closingRepository) {
        return new ClosingService(closingRepository);
    }

    // ── Use Cases ──────────────────────────────────────────────

    @Bean
    AccountUseCase accountUseCase(AccountService accountService, BalanceService balanceService) {
        return new AccountUseCase(accountService, balanceService);
    }

    @Bean
    MetadataUseCase metadataUseCase(
            TagService tagService,
            ClosingService closingService,
            CategoryService categoryService,
            CostCenterService costCenterService,
            TransactionService transactionService
    ) {
        return new MetadataUseCase(tagService, closingService, categoryService, costCenterService, transactionService);
    }

    @Bean
    TransactionUseCase transactionUseCase(TransactionService transactionService, ClosingService closingService, CategoryService categoryService) {
        return new TransactionUseCase(transactionService, closingService, categoryService);
    }

    // ── Event Listeners ──────────────────────────────────────────

    @Bean
    AccountEventListener accountEventListener(AccountService accountService) {
        return new AccountEventListener(accountService);
    }

    @Bean
    TransactionEventListener transactionEventListener(
            AccountService accountService,
            BalanceService balanceService,
            TransactionService transactionService
    ) {
        return new TransactionEventListener(accountService, balanceService, transactionService);
    }

    // ── Facade ────────────────────────────────────────────────────

    @Bean
    public MonetaryContext monetaryContext(
            AccountUseCase ucAccount,
            MetadataUseCase ucMetadata,
            TransactionUseCase ucTransaction,
            AccountEventListener accountEventListener,
            TransactionEventListener transactionEventListener
    ) {
        MessageBus.subscribe(accountEventListener);
        MessageBus.subscribe(transactionEventListener);
        return new MonetaryContext(ucAccount, ucTransaction, ucMetadata);
    }
}
