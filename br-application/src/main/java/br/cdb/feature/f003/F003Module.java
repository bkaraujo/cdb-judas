package br.cdb.feature.f003;

import br.cdb.feature.f003._0_domain.UserTagRepository;
import br.cdb.feature.f003._0_domain.UserTransactionTagRepository;
import br.cdb.feature.f003._2_infrastructure.persistence.UserTagJDBCRepository;
import br.cdb.feature.f003._2_infrastructure.persistence.UserTransactionTagJDBCRepository;
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
 * Módulo CDI da fatia {@code f008} (tags). Produtores recebem {@link DataSource} sem usá-lo no corpo
 * só para forçar o CDI a criar o schema antes do adaptador JDBC — a dependência real fica escondida
 * dentro do {@link Registry}.
 */
@NullMarked
@ApplicationScoped
public class F003Module {

    @Produces
    @Singleton
    public UserTagRepository userTagRepository() {
        return Registry.tryGet(UserTagRepository.class, UserTagJDBCRepository::new);
    }

    @Produces
    @Singleton
    public UserTransactionTagRepository userTransactionTagRepository() {
        return Registry.tryGet(UserTransactionTagRepository.class, UserTransactionTagJDBCRepository::new);
    }

    void onStart(@Observes @Priority(3) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
