package br.cdb.feature.f000;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.cdb.feature.f000._2_infrastructure.persistence.CachingPersonRepository;
import br.cdb.feature.f000._2_infrastructure.persistence.PersonJDBCRepository;
import br.cdb.feature.f000._2_infrastructure.service.SseService;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da fatia {@code f000} (kernel). Sem CDI: classe pura montada pelo {@link Context} de
 * {@code br-commons} e inicializada por {@code F999Module.FeatureBootstrap}, na ordem da lista de
 * módulos.
 */
@NullMarked
public class F000Module implements Lifecycle {

    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F000_PERSON (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    TXT_LOCALE VARCHAR(20) NOT NULL,
                    TXT_LANGUAGE VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE SYS_USER (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_USERNAME VARCHAR(120) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE SYS_USER_CREDENTIAL (
                    ID CHAR(36) PRIMARY KEY,
                    COD_USER CHAR(36) NOT NULL,
                    TXT_PASSWORD VARCHAR(255) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F000_PREFERENCES (
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_KEY VARCHAR(20) NOT NULL,
                    TXT_VALUE VARCHAR(60),
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL,
                    PRIMARY KEY (COD_PERSON, TXT_KEY)
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(SSE.class, SseService::new);
        Context.set(PersonRepository.class, () -> new CachingPersonRepository(new PersonJDBCRepository()));
        // Os dois services alcançados por Context.get() estrito (o par ReadUseCase/WriteUseCase);
        // as demais engines resolvem suas portas em campo e o Context as instancia sozinho via tryGet.
        Context.set(PersonService.class, () -> new PersonService(Context.get(PersonRepository.class)));

        return Result.success();
    }
}
