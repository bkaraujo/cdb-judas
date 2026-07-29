package br.cdb.feature.f009;

import br.commons.Logger;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApplicationScoped
public class F009Module {

    void onStart(@Observes @Priority(2) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
    }
}
