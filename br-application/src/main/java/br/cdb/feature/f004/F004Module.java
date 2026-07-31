package br.cdb.feature.f004;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TagJDBCRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TransactionTagJDBCRepository;
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
                """,
                """
                CREATE TABLE F004_TRANSACTION_TAG (
                    COD_TRANSACTION CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_TAG CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_TRANSACTION, COD_PERSON, COD_TAG)
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(TagRepository.class, TagJDBCRepository::new);
        Context.set(TransactionTagRepository.class, TransactionTagJDBCRepository::new);

        return Result.success();
    }
}
