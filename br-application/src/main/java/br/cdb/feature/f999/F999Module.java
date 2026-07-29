package br.cdb.feature.f999;

import br.cdb.feature.f000._1_application.UserService;
import br.cdb.feature.f999._0_domain.DeletionQueueRepository;
import br.cdb.feature.f999._2_infrastructure.persistence.DeletionQueueJDBCRepository;
import br.commons.Logger;
import br.commons.Registry;
import br.commons.Result;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.tools.Strings;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@ApplicationScoped
@RequiredArgsConstructor
public class F999Module {

    private final UserService service;

    /** {@code dataSource} não é usado no corpo — só força o CDI a criar o schema antes do adaptador
     *  JDBC introspectar a tabela (mesmo truque de {@code ContextBridge.personRepository}/{@code
     *  userRepository}). Sem isso, o {@code @Scheduled} de {@code DeletionQueuePurgeJob} pode fazer o
     *  scheduler instanciar esta porta cedo demais no boot — {@code JDBCRepository.introspect()} então
     *  falha contra um banco sem schema e aborta a JVM ({@code Logger.fatal}). */
    @Produces
    @Singleton
    public DeletionQueueRepository deletionQueueRepository(DataSource dataSource) {
        return Registry.tryGet(DeletionQueueRepository.class, DeletionQueueJDBCRepository::new);
    }

    void onStart(@Observes @Priority(999) StartupEvent ev) {
        Logger.debug("Iniciando módulo..");

        List
                .of("admin")
                .forEach(userName -> {
                    val userPassword = userName.toCharArray();
                    Registry.get(DataSource.class).transaction(_ -> {
                        switch (service.createUser(userName, Strings.EMPTY, userPassword)) {
                            case Result.Failure(var error) -> Logger.warn(error.toString());
                            case Result.Success(var _) -> Logger.info("Usuário criado com sucesso");
                        }

                        return Result.success();
                    });
                });
    }
}
