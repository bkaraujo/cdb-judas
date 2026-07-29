package br.cdb.feature.f002;

import br.cdb.feature.f002._0_domain.ClosingRepository;
import br.cdb.feature.f002._0_domain.UserAccountRepository;
import br.cdb.feature.f002._1_application.ClosingService;
import br.cdb.feature.f002._2_infrastructure.persistence.ClosingJDBCRepository;
import br.cdb.feature.f002._2_infrastructure.persistence.UserAccountJDBCRepository;
import br.commons.Logger;
import br.commons.Registry;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApplicationScoped
public class F002Module {

    @Produces
    @Singleton
    public UserAccountRepository userAccountRepository() {
        return Registry.tryGet(UserAccountRepository.class, UserAccountJDBCRepository::new);
    }

    @Produces
    @Singleton
    ClosingService closingService(ClosingRepository closingRepository) {
        return new ClosingService(closingRepository);
    }

    @Produces
    @Singleton
    public ClosingRepository closingRepository() {
        return Registry.tryGet(ClosingRepository.class, ClosingJDBCRepository::new);
    }

    void onStart(@Observes @Priority(2) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
