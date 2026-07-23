package br.cdb.feature.f002;

import br.cdb.feature.f002._0_domain.UserAccountRepository;
import br.cdb.feature.f002._2_infrastructure.persistence.UserAccountJDBCRepository;
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
 * Módulo CDI da fatia {@code f002} (accounts + cards + balance). {@link #userAccountRepository}
 * recebe {@link DataSource} sem usá-lo no corpo só para forçar o CDI a criar o schema antes do
 * adaptador JDBC — a dependência real fica escondida dentro do {@link Registry}.
 */
@NullMarked
@ApplicationScoped
public class F002Module {

    @Produces
    @Singleton
    public UserAccountRepository userAccountRepository() {
        return Registry.tryGet(UserAccountRepository.class, UserAccountJDBCRepository::new);
    }

    void onStart(@Observes @Priority(2) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
