package br.cdb.feature.f002._2_infrastructure;

import br.cdb.feature.f002._0_domain.UserAccountRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f002} (accounts + cards + balance). {@link #userAccountRepository}
 * recebe {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do
 * adaptador JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@Singleton
public class F002Module {

    @Produces
    @Singleton
    public UserAccountRepository userAccountRepository(DataSource dataSource) {
        return Registry.tryGet(UserAccountRepository.class, UserAccountJDBCRepository::new);
    }
}
