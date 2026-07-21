package br.cdb.feature.f005._2_infrastructure;

import br.cdb.feature.f005._0_domain.UserTransactionRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f005} (transactions + transfer). {@link #userTransactionRepository}
 * recebe {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do
 * adaptador JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@Singleton
public class F005Module {

    @Produces
    @Singleton
    public UserTransactionRepository userTransactionRepository(DataSource dataSource) {
        return Registry.tryGet(UserTransactionRepository.class, UserTransactionJDBCRepository::new);
    }
}
