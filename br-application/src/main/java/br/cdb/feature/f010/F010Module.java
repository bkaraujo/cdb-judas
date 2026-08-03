package br.cdb.feature.f010;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.cdb.feature.f010._2_infrastructure.persistence.ImportRuleJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Módulo da fatia {@code f010} (regras de nomenclatura). O adaptador JDBC é construído aqui, no
 * {@link #initialize()} — depois do {@code DataSource} publicado por {@code CoreModule} (o
 * construtor de {@code JDBCRepository} introspecta a tabela, então exige schema já criado).
 */
@NullMarked
public class F010Module implements Lifecycle {
    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F010_IMPORT_RULE (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    COD_ACCOUNT CHAR(36),
                    COD_CATEGORY CHAR(36),
                    COD_COST_CENTER CHAR(36),
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(ImportRuleRepository.class, ImportRuleJDBCRepository::new);

        return Result.success();
    }
}
