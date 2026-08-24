package br.cdb.feature.f010;

import br.cdb.core.persistence.Database;
import br.cdb.core.persistence.migration.F010ImportRuleTriggerMigration;
import br.cdb.feature.f010._0_domain.repository.ImportRuleRepository;
import br.cdb.feature.f010._0_domain.repository.ImportRuleTriggerRepository;
import br.cdb.feature.f010._2_infrastructure.persistence.ImportRuleJDBCRepository;
import br.cdb.feature.f010._2_infrastructure.persistence.ImportRuleTriggerJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
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
                    TXT_LABEL VARCHAR(255) NOT NULL,
                    COD_ACCOUNT CHAR(36),
                    COD_CATEGORY CHAR(36),
                    FLG_PLANNED CHAR(1),
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F010_IMPORT_RULE_TRIGGER (
                    COD_RULE CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_TRIGGER VARCHAR(255) NOT NULL,
                    PRIMARY KEY (COD_RULE, TXT_TRIGGER)
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        F010ImportRuleTriggerMigration.apply(Context.get(DataSource.class));
        Database.initialize(model());

        Context.set(ImportRuleRepository.class, ImportRuleJDBCRepository::new);
        Context.set(ImportRuleTriggerRepository.class, ImportRuleTriggerJDBCRepository::new);

        return Result.success();
    }
}
