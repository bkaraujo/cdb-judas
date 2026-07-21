package br.cdb.feature.f001._2_infrastructure;

import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f001} (self/preferences). {@link #preferencesRepository} recebe
 * {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do adaptador
 * JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@Singleton
public class F001Module {

    @Produces
    @Singleton
    public PreferencesRepository preferencesRepository(DataSource dataSource) {
        return Registry.tryGet(PreferencesRepository.class, PreferencesJDBCRepository::new);
    }
}
