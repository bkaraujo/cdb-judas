package br.cdb.core;

import br.cdb.context.monetary.MonetaryContext;
import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.context.people.PeopleBootstrap;
import br.cdb.context.people.PeopleContext;
import br.cdb.context.people._0_domain.repository.PersonRepository;
import br.cdb.core.web.security.UserRepository;
import br.cdb.infra.persistence.CachingPersonRepository;
import br.cdb.infra.persistence.CachingUserRepository;
import br.cdb.infra.persistence.Database;
import br.cdb.infra.persistence.core.AccountLimitMigration;
import br.cdb.infra.persistence.core.DuplicateCategoryMigration;
import br.cdb.infra.persistence.core.FeatureSchemaMigration;
import br.cdb.infra.persistence.core.LegacyCardMigration;
import br.cdb.infra.persistence.features.UserAccountBalanceJDBCRepository;
import br.cdb.infra.persistence.monetary.AccountJDBCRepository;
import br.cdb.infra.persistence.monetary.CardJDBCRepository;
import br.cdb.infra.persistence.monetary.CostCenterJDBCRepository;
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
 * <p>Os métodos {@code @Produces} de repositório abaixo recebem {@link DataSource} como parâmetro
 * mesmo sem usá-lo no corpo — força o CDI a produzir o {@code DataSource} (e criar o schema) antes
 * de qualquer adaptador JDBC ser construído. A dependência real (adaptador → {@code DataSource}) é
 * escondida dentro de {@link Registry}, invisível ao grafo de injeção do CDI; sem esse parâmetro
 * "morto" a ordem de construção não é garantida e o adaptador pode tentar ler um {@code DataSource}
 * que ainda não existe no {@link Registry}.
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
    public AccountRepository accountRepository(DataSource dataSource) {
        return Registry.tryGet(AccountRepository.class, AccountJDBCRepository::new);
    }

    @Produces
    @Singleton
    public BalanceRepository balanceRepository(DataSource dataSource) {
        return Registry.tryGet(BalanceRepository.class, UserAccountBalanceJDBCRepository::new);
    }

    @Produces
    @Singleton
    public CostCenterRepository costCenterRepository(DataSource dataSource) {
        return Registry.tryGet(CostCenterRepository.class, CostCenterJDBCRepository::new);
    }

    @Produces
    @Singleton
    public TransactionRepository transactionRepository(DataSource dataSource) {
        return Registry.tryGet(TransactionRepository.class, TransactionJDBCRepository::new);
    }

    @Produces
    @Singleton
    public CardRepository cardRepository(DataSource dataSource) {
        return Registry.tryGet(CardRepository.class, CardJDBCRepository::new);
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
    public MonetaryContext monetaryContext(
            AccountRepository accountRepository,
            BalanceRepository balanceRepository,
            CostCenterRepository costCenterRepository,
            TransactionRepository transactionRepository,
            CardRepository cardRepository
    ) {
        return MonetaryContext.instance();
    }

    @Produces
    @Singleton
    public PeopleContext peopleContext(PersonRepository personRepository) {
        PeopleBootstrap.register();
        return Registry.get(PeopleContext.class);
    }
}
