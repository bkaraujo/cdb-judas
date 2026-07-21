package br.cdb.feature.f008._2_infrastructure;

import br.cdb.feature.f008._0_domain.UserTagRepository;
import br.cdb.feature.f008._0_domain.UserTransactionTagRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f008} (tags). Produtores recebem {@link DataSource} sem usá-lo no corpo
 * só para forçar o CDI a criar o schema antes do adaptador JDBC — a dependência real fica escondida
 * dentro do {@link Registry}.
 */
@NullMarked
@Singleton
public class F008Module {

    @Produces
    @Singleton
    public UserTagRepository userTagRepository(DataSource dataSource) {
        return Registry.tryGet(UserTagRepository.class, UserTagJDBCRepository::new);
    }

    @Produces
    @Singleton
    public UserTransactionTagRepository userTransactionTagRepository(DataSource dataSource) {
        return Registry.tryGet(UserTransactionTagRepository.class, UserTransactionTagJDBCRepository::new);
    }
}
