package br.cdb.feature.f007._2_infrastructure;

import br.cdb.feature.f007._0_domain.UserCategoryRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f007} (categories). {@link #userCategoryRepository} recebe
 * {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do adaptador
 * JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@Singleton
public class F007Module {

    @Produces
    @Singleton
    public UserCategoryRepository userCategoryRepository(DataSource dataSource) {
        return Registry.tryGet(UserCategoryRepository.class, UserCategoryJDBCRepository::new);
    }
}
