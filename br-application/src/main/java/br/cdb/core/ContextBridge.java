package br.cdb.core;

import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.context.people.PeopleBootstrap;
import br.cdb.context.people.PeopleContext;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.core.security.UserRepository;
import br.cdb.infra.persistence.CachingPersonRepository;
import br.cdb.infra.persistence.CachingUserRepository;
import br.cdb.infra.persistence.Database;
import br.cdb.infra.persistence.core.AccountLimitMigration;
import br.cdb.infra.persistence.core.ContextMergeMigration;
import br.cdb.infra.persistence.core.DuplicateCategoryMigration;
import br.cdb.infra.persistence.core.FeatureSchemaMigration;
import br.cdb.infra.persistence.core.LegacyCardMigration;
import br.cdb.infra.persistence.features.UserAccountBalanceJDBCRepository;
import br.cdb.infra.persistence.monetary.AccountJDBCRepository;
import br.cdb.infra.persistence.monetary.CostCenterJDBCRepository;
import br.cdb.infra.persistence.monetary.CreditCardJDBCRepository;
import br.cdb.infra.persistence.monetary.TransactionJDBCRepository;
import br.cdb.infra.persistence.person.PersonJDBCRepository;
import br.cdb.infra.persistence.security.UserJDBCRepository;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

/**
 * Costura entre a borda Quarkus/CDI (feature) e os contextos ligados por {@link Registry}.
 *
 * <p>As portas de repositório dos contextos são publicadas no {@link Registry} em
 * {@link #initDataSource(StartupEvent, DataSource)} — depois do {@code DataSource} existir
 * (os adaptadores {@code *JDBCRepository} o resolvem no construtor) e antes de qualquer
 * requisição HTTP construir um use case de contexto via {@code MonetaryUseCases.uc*()}.
 * Só {@link UserRepository}/{@link PersonRepository} continuam expostos também como beans CDI:
 * têm consumidores injetados (login/perfil/seed) e caches que os testes precisam limpar.
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
            DuplicateCategoryMigration.apply(datasource);
            ContextMergeMigration.apply(datasource);

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
     * Força a criação do {@link DataSource} (e do schema) no startup, antes de qualquer query, e
     * publica os adaptadores JDBC nas portas de repositório dos contextos. A prioridade baixa
     * garante execução antes de observers de seed (ex.: {@code UserSeeder}).
     */
    void initDataSource(@Observes @Priority(1) StartupEvent event, DataSource dataSource) {
        Registry.tryGet(AccountRepository.class, AccountJDBCRepository::new);
        Registry.tryGet(BalanceRepository.class, UserAccountBalanceJDBCRepository::new);
        Registry.tryGet(CostCenterRepository.class, CostCenterJDBCRepository::new);
        Registry.tryGet(TransactionRepository.class, TransactionJDBCRepository::new);
        Registry.tryGet(CreditCardRepository.class, CreditCardJDBCRepository::new);
        Registry.tryGet(PersonRepository.class, () -> new CachingPersonRepository(new PersonJDBCRepository()));
        Registry.tryGet(UserRepository.class, () -> new CachingUserRepository(new UserJDBCRepository()));
    }

    @Produces
    @Singleton
    public PersonRepository personRepository(DataSource dataSource) {
        return Registry.tryGet(PersonRepository.class, () -> new CachingPersonRepository(new PersonJDBCRepository()));
    }

    @Produces
    @Singleton
    public UserRepository userRepository(DataSource dataSource) {
        return Registry.tryGet(UserRepository.class, () -> new CachingUserRepository(new UserJDBCRepository()));
    }

    @Produces
    @Singleton
    public PeopleContext peopleContext(PersonRepository personRepository) {
        PeopleBootstrap.register();
        return Registry.get(PeopleContext.class);
    }
}
