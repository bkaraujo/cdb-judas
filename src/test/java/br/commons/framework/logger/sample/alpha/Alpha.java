package br.commons.framework.logger.sample.alpha;

import br.commons.Logger;
import org.jspecify.annotations.NullMarked;

/** Emissor de logs num pacote distinto, para exercitar overrides de nível por pacote. */
@NullMarked
public final class Alpha {
    private Alpha() {}

    public static void debug(String message) { Logger.debug(message); }
    public static void info(String message)  { Logger.info(message); }
    public static void warn(String message)  { Logger.warn(message); }
}
