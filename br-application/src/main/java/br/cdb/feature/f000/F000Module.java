package br.cdb.feature.f000;

import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f000._2_infrastructure.service.SseService;
import br.commons.Logger;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApplicationScoped
public class F000Module {

    @Produces
    @Singleton
    public SSE sse() {
        return new SseService();
    }

    void onStart(@Observes @Priority(0) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
