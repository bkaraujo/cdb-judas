package br.community.context.monetary;

import br.commons.MessageBus;
import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.port.CreditCardStatementTextExtractor;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.monetary._1_application.event.TransactionEventListener;
import br.community.context.monetary._1_application.service.*;
import br.community.context.monetary._2_infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@NullMarked
public class MonetaryModule {

    private static final int MAX_STATEMENT_PAGES = 50;
    private static final long MAX_STATEMENT_FILE_BYTES = 10L * 1024 * 1024;

    // ── Repositories ──────────────────────────────────────────────

    @Bean
    AccountRepository accountRepository(ObjectMapper mapper, Storage storage) {
        return new AccountJsonRepository(mapper, storage);
    }

    @Bean
    CategoryRepository categoryRepository(ObjectMapper mapper, Storage storage) {
        return new CategoryJsonRepository(mapper, storage);
    }

    @Bean
    CostCenterRepository costCenterRepository(ObjectMapper mapper, Storage storage) {
        return new CostCenterJsonRepository(mapper, storage);
    }

    @Bean
    BalanceRepository balanceRepository(ObjectMapper mapper, Storage storage) {
        return new MonthlyBalanceJsonRepository(mapper, storage);
    }

    @Bean
    TagRepository tagRepository(ObjectMapper mapper, Storage storage) {
        return new TagJsonRepository(mapper, storage);
    }

    @Bean
    TransactionRepository transactionRepository(ObjectMapper mapper, Storage storage) {
        return new TransactionJsonRepository(mapper, storage);
    }

    @Bean
    ClosingRepository closingRepository(ObjectMapper mapper, Storage storage) {
        return new ClosingJsonRepository(mapper, storage);
    }

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

    @Bean
    CreditCardStatementTextExtractor creditCardStatementTextExtractor() {
        return new PdfBoxCreditCardStatementTextExtractor(MAX_STATEMENT_PAGES);
    }

    @Bean
    IssuerDetector issuerDetector() {
        return new IssuerDetector();
    }

    @Bean
    CreditCardStatementParserRegistry creditCardStatementParserRegistry() {
        return new CreditCardStatementParserRegistry(
                new SantanderCreditCardStatementParser(), new BtgCreditCardStatementParser());
    }

    @Bean
    CardMatcher cardMatcher() {
        return new CardMatcher();
    }

    @Bean
    GroupSignature groupSignature() {
        return new GroupSignature();
    }

    @Bean
    InstallmentExpander installmentExpander(GroupSignature groupSignature) {
        return new InstallmentExpander(groupSignature);
    }

    @Bean
    CategoryGuesser categoryGuesser() {
        return new CategoryGuesser();
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    CreditCardStatementImportUseCase creditCardStatementImportUseCase(
            CreditCardStatementTextExtractor extractor,
            IssuerDetector issuerDetector,
            CreditCardStatementParserRegistry parsers,
            AccountService accountService,
            CardMatcher cardMatcher,
            InstallmentExpander installmentExpander,
            GroupSignature groupSignature,
            TransactionService transactionService,
            CategoryGuesser categoryGuesser,
            CategoryService categoryService,
            Clock clock) {
        return new CreditCardStatementImportUseCase(
                extractor, issuerDetector, parsers, accountService, cardMatcher,
                installmentExpander, groupSignature, transactionService,
                categoryGuesser, categoryService, clock, MAX_STATEMENT_FILE_BYTES);
    }

    // ── Event Listeners ──────────────────────────────────────────

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
            CreditCardStatementImportUseCase ucImport,
            TransactionEventListener transactionEventListener
    ) {
        MessageBus.subscribe(transactionEventListener);
        return new MonetaryContext(ucAccount, ucTransaction, ucMetadata, ucImport);
    }
}
