package br.cdb.feature.f010;

import br.cdb.feature.f000._1_application.UserService;
import br.commons.Logger;
import br.commons.Result;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@NullMarked
@ApplicationScoped
@RequiredArgsConstructor
public class F010Module {

    private final UserService userService;

    void onStart(@Observes @Priority(10) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");
        val result = userService.createUser("admin", "", "admin".toCharArray());
        switch (result) {
            case Result.Failure (var error) -> Logger.warn(error.toString());
            case Result.Success (var _) ->  Logger.info("Usuário criado com sucesso");
        };
    }
}
