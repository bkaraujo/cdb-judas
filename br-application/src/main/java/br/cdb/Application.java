package br.cdb;

import br.commons.Logger;
import br.commons.RT;
import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.channel.ConsoleChannel;
import br.commons.tools.Strings;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@QuarkusMain
public class Application implements QuarkusApplication {

    /** Prefixo das variáveis de ambiente que configuram o log. */
    private static final String LOG_LEVEL_PREFIX = "APP_LOGLEVEL_";

    /** Variável do nível raiz: {@code APP_LOGLEVEL_ROOT}. */
    private static final String LOG_LEVEL_ROOT = LOG_LEVEL_PREFIX + "ROOT";

    public static void main(String[] args) {
        RT.packages.addAll(List.of(
                "br.commons.framework.logger.",
                "br.commons.Logger",
                "br.commons.tools.Meta"
        ));

        configureLogging();

        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) {
        Quarkus.waitForExit();
        return 0;
    }

    /**
     * Configura o log a partir de variáveis de ambiente.
     *
     * <ul>
     *   <li>{@code APP_LOGLEVEL_ROOT=<LEVEL>} — nível raiz (padrão {@link LogLevel#INFO}).</li>
     *   <li>{@code APP_LOGLEVEL_<pattern>=<LEVEL>} — nível por pacote. O {@code <pattern>} vem
     *       em maiúsculas com {@code _} no lugar de {@code .}, ex.:
     *       {@code APP_LOGLEVEL_IO_QUARKUS=WARN} configura {@code io.quarkus}.</li>
     * </ul>
     *
     * Valores inválidos são ignorados com um aviso, mantendo o padrão.
     */
    private static void configureLogging() {
        Logger.channel(new ConsoleChannel());
        Logger.level(parseLevel(System.getenv(LOG_LEVEL_ROOT), LogLevel.INFO));

        System.getenv().forEach((key, value) -> {
            if (!key.startsWith(LOG_LEVEL_PREFIX)) return;
            if (key.equals(LOG_LEVEL_ROOT)) return;

            val pattern = Strings.lower(key.substring(LOG_LEVEL_PREFIX.length())).replace('_', '.');

            try { Logger.level(pattern, LogLevel.valueOf(Strings.upper(value))); }
            catch (IllegalArgumentException e) { Logger.warn("Nível de log inválido para %s: %s", key, value); }
        });
    }

    private static LogLevel parseLevel(@Nullable String value, LogLevel fallback) {
        if (value == null) return fallback;

        try { return LogLevel.valueOf(Strings.upper(value)); }
        catch (IllegalArgumentException e) { Logger.warn("Nível de log inválido: %s", value); return fallback; }
    }

}
