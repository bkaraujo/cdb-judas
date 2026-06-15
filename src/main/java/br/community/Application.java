package br.community;

import br.commons.Logger;
import br.commons.Platform;
import br.commons.RT;
import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.channel.ConsoleChannel;
import br.commons.tools.Strings;
import br.community.context.ContextModule;
import br.community.feature.FeatureModule;
import br.community.infra.InfraConfigs;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;

import java.util.List;

@NullMarked
@SpringBootApplication(scanBasePackages = {"br.community"})
@ConfigurationPropertiesScan
@Import({InfraConfigs.class, ContextModule.class, FeatureModule.class})
public class Application {

    /** Prefixo das variáveis de ambiente que configuram o log. */
    private static final String LOG_LEVEL_PREFIX = "APP_LOGLEVEL_";

    /** Variável do nível raiz: {@code APP_LOGLEVEL_ROOT}. */
    private static final String LOG_LEVEL_ROOT = LOG_LEVEL_PREFIX + "ROOT";

    /** Porta HTTP assumida quando o Spring/Tomcat não define {@code server.port}. */
    private static final int DEFAULT_PORT = 8080;

    static void main(String[] args) {
        RT.packages.addAll(List.of(
                "br.commons.framework.logger.",
                "br.commons.Logger",
                "br.commons.tools.Meta"
        ));

        configureLogging();

        val app = new SpringApplication(Application.class);
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
                ensurePortAvailable(event.getEnvironment().getProperty("server.port", Integer.class, DEFAULT_PORT))
        );

        app.run(args);
    }

    /**
     * Configura o log a partir de variáveis de ambiente.
     *
     * <ul>
     *   <li>{@code APP_LOGLEVEL_ROOT=<LEVEL>} — nível raiz (padrão {@link LogLevel#INFO}).</li>
     *   <li>{@code APP_LOGLEVEL_<pattern>=<LEVEL>} — nível por pacote. O {@code <pattern>} vem
     *       em maiúsculas com {@code _} no lugar de {@code .}, ex.:
     *       {@code APP_LOGLEVEL_ORG_SPRINGFRAMEWORK=WARN} configura {@code org.springframework}.</li>
     * </ul>
     *
     * Valores inválidos são ignorados com um aviso, mantendo o padrão.
     */
    private static void configureLogging() {
        Logger.channel(new ConsoleChannel());
        Logger.level(parseLevel(System.getenv(LOG_LEVEL_ROOT), LogLevel.INFO));

        System.getenv().forEach((key, value) -> {
            if (!key.startsWith(LOG_LEVEL_PREFIX) || key.equals(LOG_LEVEL_ROOT)) return;
            val pattern = Strings.lower(key.substring(LOG_LEVEL_PREFIX.length())).replace('_', '.');

            try { Logger.level(pattern, LogLevel.valueOf(Strings.upper(value))); }
            catch (IllegalArgumentException e) { Logger.warn("Nível de log inválido para %s: %s", key, value); }
        });
    }

    /**
     * Falha cedo, com mensagem amigável, se a porta HTTP do Tomcat já estiver
     * ocupada — evita o stacktrace cru de "Port already in use" do Spring Boot.
     * Portas especiais (0 = aleatória, -1 = container desabilitado) são ignoradas.
     */
    private static void ensurePortAvailable(int port) {
        if (port <= 0 || port > 65535) return;
        if (Platform.isPortInUse(port)) {
            Logger.fatal("Porta %d já está em uso. Encerre o processo que a ocupa ou configure outra porta (server.port / SERVER_PORT).", port);
        }
    }

    private static LogLevel parseLevel(@Nullable String value, LogLevel fallback) {
        if (value == null) return fallback;

        try { return LogLevel.valueOf(Strings.upper(value)); }
        catch (IllegalArgumentException e) { Logger.warn("Nível de log inválido: %s", value); return fallback; }
    }

}
