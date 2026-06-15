package br.community.infra;

import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.security._0_domain.repository.UserRepository;
import br.community.infra.persistence.*;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Configuration
public class InfraConfigs {

    @Bean
    UserRepository userRepository(Storage storage, ObjectMapper mapper) {
        return new UserJsonRepository(storage, mapper);
    }

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

}
