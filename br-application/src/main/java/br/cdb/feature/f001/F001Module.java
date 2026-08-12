package br.cdb.feature.f001;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f001._0_domain.repository.PreferencesRepository;
import br.cdb.feature.f001._2_infrastructure.persistence.PreferencesJDBCRepository;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class F001Module implements Lifecycle {
    private static List<String> model() {
        return List.of();
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(PreferencesRepository.class, PreferencesJDBCRepository::new);

        return Result.success();
    }
}
