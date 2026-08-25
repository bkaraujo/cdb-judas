package br.cdb;

import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._1_application.service.TagService;
import br.cdb.feature.f005._0_domain.repository.CategoryRepository;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.cdb.feature.f006._0_domain.repository.TransactionCategoryRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f006._1_application.service.TransactionCategoryService;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.cdb.feature.f006._1_application.service.TransactionTagService;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.cdb.feature.f010._0_domain.repository.ImportRuleTriggerRepository;
import br.cdb.feature.f010._1_application.service.ImportRuleService;
import br.commons.MessageBus;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;

@NullMarked
public abstract class AbstractUseCaseTest {

    protected CreditCardRepository cardRepository() { return Context.get(CreditCardRepository.class); }
    protected BalanceRepository balanceRepository() { return Context.get(BalanceRepository.class); }
    protected AccountRepository accountRepository() { return Context.get(AccountRepository.class); }
    protected TransactionRepository transactionRepository() { return Context.get(TransactionRepository.class); }
    protected TransactionCategoryRepository transactionCategoryRepository() { return Context.get(TransactionCategoryRepository.class); }
    protected TransactionTagRepository transactionTagRepository() { return Context.get(TransactionTagRepository.class); }
    protected TagRepository tagRepository() { return Context.get(TagRepository.class); }
    protected CategoryRepository categoryRepository() { return Context.get(CategoryRepository.class); }
    protected PersonRepository personRepository() { return Context.get(PersonRepository.class); }
    protected ImportRuleRepository importRuleRepository() { return Context.get(ImportRuleRepository.class); }
    protected ImportRuleTriggerRepository importRuleTriggerRepository() { return Context.get(ImportRuleTriggerRepository.class); }

    @BeforeEach
    public void beforeEach() {
        MessageBus.reset();
        Context.remove(CreditCardService.class);
        Context.remove(AccountService.class);
        Context.remove(BalanceService.class);
        Context.remove(TransactionService.class);
        Context.remove(TransactionCategoryService.class);
        Context.remove(TransactionTagService.class);
        Context.remove(TagService.class);
        Context.remove(UserCategoryService.class);
        Context.remove(ImportRuleService.class);

        Context.tryGet(CreditCardRepository.class, InMemoryRepositories.Cards::new).clearCache();
        Context.tryGet(BalanceRepository.class, InMemoryRepositories.Balances::new).clearCache();
        Context.tryGet(AccountRepository.class, InMemoryRepositories.Accounts::new).clearCache();
        Context.tryGet(TransactionRepository.class, InMemoryRepositories.Transactions::new).clearCache();
        // TransactionCategoryRepository/TransactionTagRepository/TagRepository não estendem Repository
        // (não têm clearCache): em vez de limpar o fake, publica-se um novo a cada teste — o que
        // também sobrescreve o adaptador JDBC deixado por um @QuarkusTest anterior.
        Context.set(TransactionCategoryRepository.class, InMemoryRepositories.TransactionCategories::new);
        Context.set(TransactionTagRepository.class, InMemoryRepositories.TransactionTags::new);
        Context.set(TagRepository.class, InMemoryRepositories.Tags::new);
        Context.set(CategoryRepository.class, InMemoryRepositories.Categories::new);
        Context.set(br.cdb.feature.f006.F006Api.class, () -> new br.cdb.feature.f006.F006Api() {
            private br.cdb.feature.f006.F006Api.TransactionView toView(br.cdb.feature.f006._0_domain.model.Transaction t) {
                return new br.cdb.feature.f006.F006Api.TransactionView(t.id(), t.accountId(), t.description(), t.amount(),
                        t.date(), t.status(), br.cdb.feature.f005._0_domain.model.Nature.EXPENSE, t.groupId(), t.cardId());
            }
            public java.util.List<br.cdb.feature.f006.F006Api.TransactionView> transactions() {
                return Context.get(TransactionRepository.class).findAll().stream().map(this::toView).toList();
            }
            public java.util.List<br.cdb.feature.f006.F006Api.TransactionView> transactions(String status, java.time.LocalDate from, java.time.LocalDate to) { return transactions(); }
            public java.util.List<br.cdb.feature.f006.F006Api.TransactionView> transactionsByAccount(java.util.UUID id, String s, java.time.LocalDate f, java.time.LocalDate t) {
                return Context.get(TransactionRepository.class).findAll().stream()
                        .filter(x -> id.equals(x.accountId())).map(this::toView).toList();
            }
            public br.cdb.feature.f006.F006Api.TransactionDto transfer(java.util.UUID f, java.util.UUID t, java.time.LocalDate d, java.math.BigDecimal a) { return null; }
            public br.cdb.feature.f006.F006Api.TransactionDto createTransaction(java.util.UUID a, br.cdb.feature.f006.F006Api.TransactionBody b) { return null; }
            public br.cdb.feature.f006.F006Api.TransactionDto updateTransaction(java.util.UUID a, java.util.UUID t, br.cdb.feature.f006.F006Api.TransactionBody b) { return null; }
            public br.cdb.feature.f006.F006Api.TransactionDto patchStatus(java.util.UUID a, java.util.UUID t, br.cdb.feature.f006._0_domain.model.Status s, java.time.LocalDate p) { return null; }
            public void deleteTransaction(java.util.UUID a, java.util.UUID t, String m) {}
            public java.util.List<java.util.UUID> transactionIdsByCategories(java.util.Collection<java.util.UUID> c) { return java.util.List.of(); }
        });
        Context.set(PersonRepository.class, InMemoryRepositories.People::new);
        Context.set(ImportRuleRepository.class, InMemoryRepositories.ImportRules::new);
        Context.set(ImportRuleTriggerRepository.class, InMemoryRepositories.ImportRuleTriggers::new);

        // O par ReadUseCase/WriteUseCase resolve o service com Context.get() estrito (em produção quem registra é
        // F000Module): re-registra sobre os fakes acima, depois de removido.
        Context.set(PersonService.class, () -> new PersonService(Context.get(PersonRepository.class)));
        // O par de f000 guarda os dois services acima em campo: precisa cair junto com eles.
        Context.remove(br.cdb.feature.f000._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f000._1_application.usecase.WriteUseCase.class);
    }

}


