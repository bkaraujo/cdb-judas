package br.cdb.feature.f002;

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

@NullMarked
public class F002Module implements Lifecycle {

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Context.set(AccountRepository.class, AccountJDBCRepository::new);
        Context.set(BalanceRepository.class, UserAccountBalanceJDBCRepository::new);
        Context.set(ClosingRepository.class, ClosingJDBCRepository::new);
        Context.set(ClosingService.class, () -> new ClosingService(Context.get(ClosingRepository.class)));

        return Result.success();
    }
}
