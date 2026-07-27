package br.cdb.feature.f001;

import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.cdb.feature.f001._2_infrastructure.persistence.PreferencesJDBCRepository;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo CDI da fatia {@code f001} (self/preferences). {@link #preferencesRepository} recebe
 * {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do adaptador
 * JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@ApplicationScoped
public class F001Module {

    @Produces
    @Singleton
    public PreferencesRepository preferencesRepository(DataSource dataSource) {
        return Registry.tryGet(PreferencesRepository.class, PreferencesJDBCRepository::new);
    }

    void onStart(@Observes @Priority(1) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
