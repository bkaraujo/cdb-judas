package br.cdb.feature.f004;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TagJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da fatia {@code f004} (tags). Os adaptadores JDBC são construídos aqui, no
 * {@link #initialize()} — depois do {@code DataSource} publicado por {@code CoreModule} (o
 * construtor de {@code JDBCRepository} introspecta a tabela, então exige schema já criado).
 *
 * <p>{@code F006_TRANSACTION_TAG} (DDL, leitura e escrita) é de {@code f006}
 * ({@code TransactionTagRepository}) — é vínculo da transação, não da tag; {@code f004} só publica
 * {@code TagEvents.Deleted}, consumido em {@code F006Module}.</p>
 */
@NullMarked
public class F004Module implements Lifecycle {
    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F004_TAG (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    TXT_COLOR VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(TagRepository.class, TagJDBCRepository::new);

        return Result.success();
    }
}
