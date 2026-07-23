package br.cdb.feature.f005;

import br.cdb.feature.f005._0_domain.UserTransactionRepository;
import br.cdb.feature.f005._2_infrastructure.persistence.UserTransactionJDBCRepository;
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
 * Módulo CDI da fatia {@code f005} (transactions + transfer). {@link #userTransactionRepository}
 * recebe {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do
 * adaptador JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@ApplicationScoped
public class F005Module {

    @Produces
    @Singleton
    public UserTransactionRepository userTransactionRepository() {
        return Registry.tryGet(UserTransactionRepository.class, UserTransactionJDBCRepository::new);
    }

    void onStart(@Observes @Priority(5) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
