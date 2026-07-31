package br.cdb.feature.f002;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._0_domain.repository.ClosingRepository;
import br.cdb.feature.f002._1_application.service.ClosingService;
import br.cdb.feature.f002._2_infrastructure.persistence.AccountJDBCRepository;
import br.cdb.feature.f002._2_infrastructure.persistence.ClosingJDBCRepository;
import br.cdb.feature.f002._2_infrastructure.persistence.UserAccountBalanceJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class F002Module implements Lifecycle {

    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F002_ACCOUNT (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    TXT_TYPE CHAR(36) NOT NULL REFERENCES SYS_ACCOUNT_TYPE(ID),
                    TXT_NAME VARCHAR(80) NOT NULL,
                    TXT_COLOR VARCHAR(20),
                    DEC_CREDIT_LIMIT DECIMAL(19, 2),
                    DEC_OVERDRAFT_LIMIT DECIMAL(19, 2),
                    NUM_CLOSING_DAY INT,
                    NUM_DUE_DAY INT,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F002_BALANCE (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    NUM_PERIOD INT NOT NULL,
                    DEC_BALANCE DECIMAL(19, 2) NOT NULL,
                    FLG_DIRTY CHAR(1) NOT NULL
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(AccountRepository.class, AccountJDBCRepository::new);
        Context.set(BalanceRepository.class, UserAccountBalanceJDBCRepository::new);
        Context.set(ClosingRepository.class, ClosingJDBCRepository::new);
        Context.set(ClosingService.class, () -> new ClosingService(Context.get(ClosingRepository.class)));

        return Result.success();
    }
}
