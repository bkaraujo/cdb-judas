package logsample.beta;

import br.commons.Logger;
import org.jspecify.annotations.NullMarked;

/** Emissor de logs num segundo pacote, sem override — deve seguir o nível global. */
@NullMarked
public final class Beta {
    private Beta() {}

    public static void debug(String message) { Logger.debug(message); }
    public static void info(String message)  { Logger.info(message); }
}
