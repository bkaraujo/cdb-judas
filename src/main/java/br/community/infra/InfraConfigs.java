package br.community.infra;

import br.community.context.monetary._0_domain.repository.*;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.core.web.security.UserRepository;
import br.community.feature.user.accounts.closing.ClosingRepository;
import br.community.feature.user.accounts.transactions.UserTransactionRepository;
import br.community.feature.user.categories.UserCategoryRepository;
import br.community.feature.user.tags.UserTagRepository;
import br.community.infra.persistence.*;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NullMarked
@Configuration
@DependsOn("dataSource")
public class InfraConfigs {

    @Bean
    ReadWriteLock databaseLock() {
        return new ReentrantReadWriteLock(false);
    }

    @Bean
    UserRepository userRepository(ReadWriteLock databaseLock) {
        return new CachingUserRepository(new UserJDBCRepository(), databaseLock);
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
    PersonRepository personRepository(ReadWriteLock databaseLock) {
        return new CachingPersonRepository(new PersonJDBCRepository(), databaseLock);
    }
}
