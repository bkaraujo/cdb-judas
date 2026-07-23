package br.cdb.feature.f999;

import br.cdb.feature.f000._1_application.UserService;
import br.commons.Logger;
import br.commons.Result;
import br.commons.tools.Strings;
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
public class F999Module {

    private final UserService service;

    void onStart(@Observes @Priority(999) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");

        val userName = "admin";
        val userPassword = userName.toCharArray();

        switch (service.createUser(userName, Strings.EMPTY, userPassword)) {
            case Result.Failure (var error) -> Logger.warn(error.toString());
            case Result.Success (var _) ->  Logger.info("Usuário criado com sucesso");
        };
    }
}
