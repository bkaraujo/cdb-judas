package br.community.infra;

import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.core.ContextBridge;
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
import br.community.infra.persistence.monetary.CostCenterJDBCRepository;
import br.community.infra.persistence.monetary.TransactionJDBCRepository;
import br.community.infra.persistence.person.PersonJDBCRepository;
import br.community.infra.persistence.security.UserJDBCRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Configuration
@Import(ContextBridge.class)
@DependsOn("dataSource")
public class InfraConfigs {

    @Bean
    UserRepository userRepository() {
        return new CachingUserRepository(new UserJDBCRepository());
    }

    @Bean
    PreferencesRepository preferencesRepository() {
        return new PreferencesJDBCRepository();
    }

    @Bean
    AccountRepository accountRepository(ObjectMapper mapper) {
        return new AccountJDBCRepository(mapper);
    }

    @Bean
    BalanceRepository balanceRepository() {
        return new UserAccountBalanceJDBCRepository();
    }

    @Bean
    UserAccountJDBCRepository userAccountRepository() {
        return new UserAccountJDBCRepository();
    }

    @Bean
    CostCenterRepository costCenterRepository() {
        return new CostCenterJDBCRepository();
    }

    @Bean
    TransactionRepository transactionRepository() {
        return new TransactionJDBCRepository();
    }

    @Bean
    UserCategoryRepository userCategoryRepository() {
        return new UserCategoryJDBCRepository();
    }

    @Bean
    UserTagRepository userTagRepository() {
        return new UserTagJDBCRepository();
    }

    @Bean
    UserTransactionRepository userTransactionRepository() {
        return new UserTransactionJDBCRepository();
    }

    @Bean
    ClosingRepository closingRepository() {
        return new ClosingJDBCRepository();
    }

    @Bean
    PersonRepository personRepository() {
        return new CachingPersonRepository(new PersonJDBCRepository());
    }
}
