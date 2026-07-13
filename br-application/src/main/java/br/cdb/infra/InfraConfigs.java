package br.cdb.infra;

import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.core.web.security.UserRepository;
import br.cdb.feature.user.accounts.closing.ClosingRepository;
import br.cdb.feature.user.accounts.transactions.UserTransactionRepository;
import br.cdb.feature.user.categories.UserCategoryRepository;
import br.cdb.feature.user.profile.PreferencesRepository;
import br.cdb.feature.user.tags.UserTagRepository;
import br.cdb.feature.user.tags.UserTransactionTagRepository;
import br.cdb.infra.persistence.CachingPersonRepository;
import br.cdb.infra.persistence.CachingUserRepository;
import br.cdb.infra.persistence.features.*;
import br.cdb.infra.persistence.monetary.AccountJDBCRepository;
import br.cdb.infra.persistence.monetary.CardJDBCRepository;
import br.cdb.infra.persistence.monetary.CostCenterJDBCRepository;
import br.cdb.infra.persistence.monetary.TransactionJDBCRepository;
import br.cdb.infra.persistence.person.PersonJDBCRepository;
import br.cdb.infra.persistence.security.UserJDBCRepository;
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
    UserTransactionTagRepository userTransactionTagRepository() {
        return new UserTransactionTagJDBCRepository();
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
