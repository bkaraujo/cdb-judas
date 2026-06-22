package br.community.infra;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.jdbc.DataSource;
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
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Configuration
public class InfraConfigs {

    @Bean
    UserRepository userRepository(DataSource dataSource) {
        return new UserJDBCRepository(dataSource);
    }

    @Bean
    AccountRepository accountRepository(ObjectMapper mapper, DataSource dataSource) {
        return new AccountJDBCRepository(dataSource, mapper);
    }

    @Bean
    BalanceRepository balanceRepository(DataSource dataSource) {
        return new UserAccountBalanceJDBCRepository(dataSource);
    }

    @Bean
    UserAccountJDBCRepository userAccountRepository(DataSource dataSource) {
        return new UserAccountJDBCRepository(dataSource);
    }

    @Bean
    CostCenterRepository costCenterRepository(DataSource dataSource) {
        return new CostCenterJDBCRepository(dataSource);
    }

    @Bean
    TransactionRepository transactionRepository(DataSource dataSource) {
        return new TransactionJDBCRepository(dataSource);
    }

    @Bean
    UserCategoryRepository userCategoryRepository(DataSource dataSource) {
        return new UserCategoryJDBCRepository(dataSource);
    }

    @Bean
    UserTagRepository userTagRepository(DataSource dataSource) {
        return new UserTagJDBCRepository(dataSource);
    }

    @Bean
    UserTransactionRepository userTransactionRepository(DataSource dataSource) {
        return new UserTransactionJDBCRepository(dataSource);
    }

    @Bean
    ClosingRepository closingRepository(ObjectMapper mapper, Storage storage) {
        return new ClosingJsonRepository(mapper, storage);
    }

    @Bean
    PersonRepository personRepository(DataSource dataSource) {
        return new PersonJDBCRepository(dataSource);
    }
}
