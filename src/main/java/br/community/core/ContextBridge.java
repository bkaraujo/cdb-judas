package br.community.core;

import br.commons.Logger;
import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import br.community.context.monetary.MonetaryBootstrap;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.repository.AccountRepository;
import br.community.context.monetary._0_domain.repository.BalanceRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.people.PeopleBootstrap;
import br.community.context.people.PeopleContext;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.infra.persistence.Database;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Costura entre a borda Spring (feature) e os contextos ligados por {@link Registry}.
 */
@NullMarked
@Configuration
public class ContextBridge {

    /**
     * Monta o {@code DataSource} H2 a partir de {@link DataSourceProperties} (file em dev, in-memory no
     * perfil de teste), cria o schema ({@link Database#model()}) e o publica no {@link Registry} para os
     * adaptadores JDBC. Bean explícito (e não inline em {@link #monetaryContext}) para que esteja registrado
     * antes da construção dos repositórios — ver {@code @DependsOn("dataSource")} em {@code InfraConfigs}.
     */
    @Bean
    public DataSource dataSource(DataSourceProperties config) {
        val properties = new JDBCProperties();
        properties.driver(config.driver());
        properties.url(config.url());
        properties.username(config.username());
        properties.password(config.password());
        properties.validationQuery("SELECT 1");
        properties.minPoolSize(5);
        properties.maxPoolSize(20);

        return Registry.tryGet(DataSource.class, () -> {
            val datasource = new DataSource(properties);
            switch (datasource.begin()) {
                case Result.Failure(var error) -> throw new IllegalStateException(error);
                case Result.Success(var transaction) -> {
                    if (transaction == null) throw new IllegalStateException("Transaction is null");
                    for (val command : Database.model()) {
                        transaction
                                .execute(command)
                                .ifFailure(reason -> Logger.error("Erro ao criar schema: %s", reason));
                    }
                    transaction.close();
                }
            }

            return datasource;
        });
    }

    @Bean
    public MonetaryContext monetaryContext(
            AccountRepository accountRepository,
            BalanceRepository balanceRepository,
            CostCenterRepository costCenterRepository,
            TransactionRepository transactionRepository
    ) {
        Registry.set(AccountRepository.class, accountRepository);
        Registry.set(BalanceRepository.class, balanceRepository);
        Registry.set(CostCenterRepository.class, costCenterRepository);
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
