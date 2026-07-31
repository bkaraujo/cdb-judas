package br.cdb.feature.f009;

import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import org.jspecify.annotations.NullMarked;

/** Módulo da fatia {@code f009} (dashboard): sem porta nem serviço próprio — lê f006 via {@code InternalApi}. */
@NullMarked
public class F009Module implements Lifecycle {

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        return Result.success();
    }
}
