package br.community.core;

import br.commons.Registry;
import br.community.context.monetary.MonetaryBootstrap;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.people.PeopleBootstrap;
import br.community.context.people.PeopleContext;
import br.community.context.people._0_domain.repository.PersonRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Costura entre a borda Spring (feature) e os contextos ligados por {@link Registry}.
 */
@NullMarked
@Configuration
public class ContextBridge {

    @Bean
    public MonetaryContext monetaryContext(
            AccountRepository accountRepository,
            BalanceRepository balanceRepository,
            CategoryRepository categoryRepository,
            CostCenterRepository costCenterRepository,
            TagRepository tagRepository,
            TransactionRepository transactionRepository
    ) {
        Registry.set(AccountRepository.class, accountRepository);
        Registry.set(BalanceRepository.class, balanceRepository);
        Registry.set(CategoryRepository.class, categoryRepository);
        Registry.set(CostCenterRepository.class, costCenterRepository);
        Registry.set(TagRepository.class, tagRepository);
        Registry.set(TransactionRepository.class, transactionRepository);

        MonetaryBootstrap.register();
        return Registry.get(MonetaryContext.class);
    }

    @Bean
    public PeopleContext peopleContext(PersonRepository personRepository) {
        Registry.set(PersonRepository.class, personRepository);
        PeopleBootstrap.register();
        return Registry.get(PeopleContext.class);
    }
}
