package br.community.core;

import br.commons.Registry;
import br.community.context.monetary.MonetaryBootstrap;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.repository.*;
import br.community.context.people.PeopleBootstrap;
import br.community.context.people.PeopleContext;
import br.community.context.people._0_domain.repository.PersonAccountRepository;
import br.community.context.people._0_domain.repository.PersonRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Costura entre a borda Spring (feature) e os contextos ligados por {@link Registry}.
 *
 * <p>No modelo híbrido acordado, {@code context/*} é ligado por {@code Registry} (DI manual)
 * e {@code feature/*} permanece em Spring. Esta classe é o único ponto onde os dois mundos se
 * encontram: ela recebe os beans de persistência do Spring ({@code InfraConfigs}), publica-os
 * como portas no {@code Registry}, dispara o composition root do contexto
 * ({@link MonetaryBootstrap#register()}) e reexpõe a {@link MonetaryContext} como bean Spring,
 * de modo que os {@code Resource}s de feature continuem injetando a facade por construtor,
 * sem mudança — preservando a regra ArchUnit {@code feature → context só via Facade}.</p>
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
    public PeopleContext peopleContext(
            PersonRepository personRepository,
            PersonAccountRepository personAccountRepository
    ) {
        Registry.set(PersonRepository.class, personRepository);
        Registry.set(PersonAccountRepository.class, personAccountRepository);
        PeopleBootstrap.register();
        return Registry.get(PeopleContext.class);
    }
}
