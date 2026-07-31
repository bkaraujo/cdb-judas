package br.cdb.feature.f001;

import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.cdb.feature.f001._2_infrastructure.persistence.PreferencesJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class F001Module implements Lifecycle {

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Context.set(PreferencesRepository.class, PreferencesJDBCRepository::new);

        return Result.success();
    }
}
