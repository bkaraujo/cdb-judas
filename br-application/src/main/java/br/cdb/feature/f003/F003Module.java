package br.cdb.feature.f003;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._1_application.cache.CreditCardCache;
import br.cdb.feature.f003._2_infrastructure.F003ApiImpl;
import br.cdb.feature.f003._2_infrastructure.persistence.CreditCardJDBCRepository;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class F003Module implements Lifecycle {


    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F003_CARD (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    TXT_LAST4 CHAR(4) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
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

        Context.set(CreditCardRepository.class, CreditCardJDBCRepository::new);
        Context.set(F003Api.class, F003ApiImpl::new);
        Context.set(CreditCardCache.class, CreditCardCache::new);

        MessageBus.subscribe(Context.get(CreditCardCache.class));

        return Result.success();
    }
}
