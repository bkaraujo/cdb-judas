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
import br.community.context.monetary._0_domain.repository.CardRepository;
import br.community.context.monetary._0_domain.repository.CostCenterRepository;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.people.PeopleBootstrap;
import br.community.context.people.PeopleContext;
import br.community.context.people._0_domain.repository.PersonRepository;
import br.community.infra.persistence.AccountLimitMigration;
import br.community.infra.persistence.Database;
import br.community.infra.persistence.FeatureSchemaMigration;
import br.community.infra.persistence.LegacyCardMigration;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Costura entre a borda Quarkus/CDI (feature) e os contextos ligados por {@link Registry}.
 */
@NullMarked
@Singleton
public class ContextBridge {

    /**
     * Monta o {@code DataSource} H2 a partir de {@link DataSourceProperties} (file em dev, in-memory no
     * perfil de teste), cria o schema ({@link Database#model()}) e o publica no {@link Registry} para os
     * adaptadores JDBC.
     */
    @Produces
    @Singleton
    public DataSource dataSource(DataSourceProperties config) {
        val properties = new JDBCProperties();
        properties.driver(config.driver());
        properties.url(config.url());
        properties.username(config.username());
        properties.password(config.password().orElse(""));
        properties.validationQuery("SELECT 1");
        properties.minPoolSize(5);
        properties.maxPoolSize(20);

        return Registry.tryGet(DataSource.class, () -> {
            val datasource = new DataSource(properties);
            LegacyCardMigration.apply(datasource);
            AccountLimitMigration.apply(datasource);
            FeatureSchemaMigration.apply(datasource);
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

    /**
     * Força a criação do {@link DataSource} (e do schema) no startup, antes de qualquer query.
     * A prioridade baixa garante execução antes de observers de seed (ex.: {@code UserSeeder}) —
     * substitui o {@code @DependsOn("dataSource")} do Spring.
     */
    void initDataSource(@Observes @Priority(1) StartupEvent event, DataSource dataSource) {
        // A injeção de DataSource basta para acionar o producer e construir o schema.
    }

    @Produces
    @Singleton
    public MonetaryContext monetaryContext(
            AccountRepository accountRepository,
            BalanceRepository balanceRepository,
            CostCenterRepository costCenterRepository,
            TransactionRepository transactionRepository,
            CardRepository cardRepository
    ) {
        Registry.set(AccountRepository.class, accountRepository);
        Registry.set(BalanceRepository.class, balanceRepository);
        Registry.set(CostCenterRepository.class, costCenterRepository);
        Registry.set(TransactionRepository.class, transactionRepository);
        Registry.set(CardRepository.class, cardRepository);

        MonetaryBootstrap.register();
        return Registry.get(MonetaryContext.class);
    }

    @Produces
    @Singleton
    public PeopleContext peopleContext(PersonRepository personRepository) {
        Registry.set(PersonRepository.class, personRepository);
        PeopleBootstrap.register();
        return Registry.get(PeopleContext.class);
    }
}
