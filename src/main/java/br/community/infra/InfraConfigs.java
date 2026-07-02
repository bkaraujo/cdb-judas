package br.community.infra;

import br.community.context.monetary._0_domain.repository.AccountLimitRepository;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.monetary._0_domain.repository.CardRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.core.web.security.UserRepository;
import br.community.feature.user.accounts.closing.ClosingRepository;
import br.community.feature.user.accounts.transactions.UserTransactionRepository;
import br.community.feature.user.categories.UserCategoryRepository;
import br.community.feature.user.profile.PreferencesRepository;
import br.community.feature.user.tags.UserTagRepository;
import br.community.infra.persistence.CachingPersonRepository;
import br.community.infra.persistence.CachingUserRepository;
import br.community.infra.persistence.features.*;
import br.community.infra.persistence.monetary.AccountJDBCRepository;
import br.community.infra.persistence.monetary.AccountLimitJDBCRepository;
import br.community.infra.persistence.monetary.CardJDBCRepository;
import br.community.infra.persistence.monetary.CostCenterJDBCRepository;
import br.community.infra.persistence.monetary.TransactionJDBCRepository;
import br.community.infra.persistence.person.PersonJDBCRepository;
import br.community.infra.persistence.security.UserJDBCRepository;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Producers CDI dos adaptadores de persistência (portas → {@code *JDBCRepository}).
 * A ordem em relação ao {@code DataSource} é garantida pelo observer de startup em
 * {@code ContextBridge} (schema criado antes de qualquer query) — ver nota em
 * {@code AuthenticationFilter} sobre por que {@code UserRepository} lá é injetado
 * de forma preguiçosa (deployment de filtros JAX-RS roda antes do {@code StartupEvent}).
 */
@NullMarked
@Singleton
public class InfraConfigs {

    @Produces
    @Singleton
    UserRepository userRepository() {
        return new CachingUserRepository(new UserJDBCRepository());
    }

    @Produces
    @Singleton
    PreferencesRepository preferencesRepository() {
        return new PreferencesJDBCRepository();
    }

    @Produces
    @Singleton
    AccountRepository accountRepository() {
        return new AccountJDBCRepository();
    }

    @Produces
    @Singleton
    BalanceRepository balanceRepository() {
        return new UserAccountBalanceJDBCRepository();
    }

    @Produces
    @Singleton
    UserAccountJDBCRepository userAccountRepository() {
        return new UserAccountJDBCRepository();
    }

    @Produces
    @Singleton
    CostCenterRepository costCenterRepository() {
        return new CostCenterJDBCRepository();
    }

    @Produces
    @Singleton
    TransactionRepository transactionRepository() {
        return new TransactionJDBCRepository();
    }

    @Produces
    @Singleton
    CardRepository cardRepository() {
        return new CardJDBCRepository();
    }

    @Produces
    @Singleton
    AccountLimitRepository accountLimitRepository() {
        return new AccountLimitJDBCRepository();
    }

    @Produces
    @Singleton
    UserCategoryRepository userCategoryRepository() {
        return new UserCategoryJDBCRepository();
    }

    @Produces
    @Singleton
    UserTagRepository userTagRepository() {
        return new UserTagJDBCRepository();
    }

    @Produces
    @Singleton
    UserTransactionRepository userTransactionRepository() {
        return new UserTransactionJDBCRepository();
    }

    @Produces
    @Singleton
    ClosingRepository closingRepository() {
        return new ClosingJDBCRepository();
    }

    @Produces
    @Singleton
    PersonRepository personRepository() {
        return new CachingPersonRepository(new PersonJDBCRepository());
    }
}
