package br.cdb.feature.f999;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f000._1_application.service.UserService;
import br.cdb.feature.f002._0_domain.DeletionQueue;
import br.cdb.feature.f999._0_domain.DeletionQueueRepository;
import br.cdb.feature.f999._1_application.DeletionQueueService;
import br.cdb.feature.f999._2_infrastructure.adapter.DeletionQueueAdapter;
import br.cdb.feature.f999._2_infrastructure.persistence.DeletionQueueJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da última fatia. Registra a fila de exclusão ({@link DeletionQueueRepository}/
 * {@link DeletionQueueService}) e provisiona o usuário {@code admin} — o que dispara em cascata o
 * seed de categorias de {@code f005}, por isso é o último da lista de {@code FeatureBootstrap}.
 *
 * <p>O adaptador JDBC da fila é construído aqui, e não sob demanda: {@code JDBCRepository} introspecta
 * a tabela no construtor, então precisa do schema já criado por {@code CoreModule} — antes, o
 * {@code @Scheduled} de {@code DeletionQueuePurgeJob} podia instanciá-lo cedo demais no boot e abortar
 * a JVM ({@code Logger.fatal}).
 */
@NullMarked
public class F999Module implements Lifecycle {

    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F999_DELETION_QUEUE (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_TYPE VARCHAR(40) NOT NULL,
                    COD_TARGET CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    NUM_ATTEMPTS INT NOT NULL,
                    FLG_LOCKED CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(DeletionQueueRepository.class, DeletionQueueJDBCRepository::new);
        Context.set(DeletionQueueService.class, () -> new DeletionQueueService(Context.get(DeletionQueueRepository.class)));
        // Depois do service: o adapter o resolve em campo. Publicado aqui (composition root) porque
        // quem consome a porta é f002.WriteUseCase, Context-wired — não há injeção CDI no caminho.
        Context.set(DeletionQueue.class, DeletionQueueAdapter::new);

        // Montado só aqui (não no FeatureBootstrap): resolve PersonRepository/UserRepository em campo,
        // então só pode ser construído depois de f000 registrar suas portas.
        val service = Context.tryGet(UserService.class);
        List
                .of("admin")
                .forEach(userName -> {
                    val userPassword = userName.toCharArray();
                    Context.get(DataSource.class).transaction(_ -> {
                        switch (service.createUser(userName, Strings.EMPTY, userPassword)) {
                            case Result.Failure(var error) -> Logger.warn(error.toString());
                            case Result.Success(var _) -> Logger.info("Usuário criado com sucesso");
                        }

                        return Result.success();
                    });
                });

        return Result.success();
    }
}
