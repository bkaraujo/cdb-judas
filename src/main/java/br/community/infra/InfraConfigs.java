package br.community.infra;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.jdbc.DataSource;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.people._0_domain.repository.PersonAccountRepository;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.core.web.security.UserRepository;
import br.community.feature.user.accounts.closing.ClosingRepository;
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
    CategoryRepository categoryRepository(DataSource dataSource) {
        return new CategoryJDBCRepository(dataSource);
    }

    @Bean
    CostCenterRepository costCenterRepository(DataSource dataSource) {
        return new CostCenterJDBCRepository(dataSource);
    }

    @Bean
    BalanceRepository balanceRepository(DataSource dataSource) {
        return new MonthlyBalanceJDBCRepository(dataSource);
    }

    @Bean
    TagRepository tagRepository(DataSource dataSource) {
        return new TagJDBCRepository(dataSource);
    }

    @Bean
    TransactionRepository transactionRepository(DataSource dataSource) {
        return new TransactionJDBCRepository(dataSource);
    }

    @Bean
    ClosingRepository closingRepository(ObjectMapper mapper, Storage storage) {
        return new ClosingJsonRepository(mapper, storage);
    }

    @Bean
    PersonRepository personRepository(DataSource dataSource) {
        return new PersonJDBCRepository(dataSource);
    }

    @Bean
    PersonAccountRepository personAccountRepository(DataSource dataSource) {
        return new PersonAccountJDBCRepository(dataSource);
    }

}
