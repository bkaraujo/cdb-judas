package br.cdb.feature.f003;

import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._2_infrastructure.persistence.CreditCardJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class F003Module implements Lifecycle {

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Context.set(CreditCardRepository.class, CreditCardJDBCRepository::new);

        return Result.success();
    }
}
