package br.cdb.feature.f006;

import br.cdb.feature.f006._0_domain.UserTransactionRepository;
import br.cdb.feature.f006._2_infrastructure.persistence.UserTransactionJDBCRepository;
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
 * Módulo CDI da fatia {@code f006} (transactions + transfer). {@link #userTransactionRepository}
 * recebe {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do
 * adaptador JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@ApplicationScoped
public class F006Module {

    @Produces
    @Singleton
    public UserTransactionRepository userTransactionRepository() {
        return Registry.tryGet(UserTransactionRepository.class, UserTransactionJDBCRepository::new);
    }

    void onStart(@Observes @Priority(6) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
