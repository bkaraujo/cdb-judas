package br.commons.framework.logger;

import br.commons.Logger;
import br.commons.framework.logger.channel.ConsoleChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O gate público de nível — o que caminho quente consulta antes de montar um argumento caro
 * (cronômetro, stack walk, formatação), já que o gate de dentro do {@code Logger.debug} só roda
 * depois de o argumento ter sido avaliado.
 */
class LoggerEnabledGateTest {

    private static final String CACHE_CLASS = "br.commons.cache.AbstractCache";
    private static final String CACHE_PKG = "br.commons.cache";

    @BeforeEach
    void setUp() {
        Logger.clearPackageFilters();
        Logger.level(LogLevel.INFO);
    }

    @AfterEach
    void tearDown() {
        Logger.clearPackageFilters();
        Logger.level(LogLevel.INFO);
        Logger.channel(new ConsoleChannel());
    }

    @Test
    void followsGlobalLevelWhenNoPackageFilter() {
        assertFalse(Logger.enabled(LogLevel.DEBUG, CACHE_CLASS));
        assertTrue(Logger.enabled(LogLevel.WARN, CACHE_CLASS));

        Logger.level(LogLevel.DEBUG);
        assertTrue(Logger.enabled(LogLevel.DEBUG, CACHE_CLASS));
    }

    @Test
    void packageOverrideWinsOverRoot() {
        Logger.level(LogLevel.WARN);
        Logger.level(CACHE_PKG, LogLevel.DEBUG);

        assertTrue(Logger.enabled(LogLevel.DEBUG, CACHE_CLASS), "override do pacote reabre DEBUG");
        assertFalse(Logger.enabled(LogLevel.DEBUG, "br.commons.platform.NativeCache"),
                "pacote sem override segue o root WARN");
    }

    @Test
    void packageOverrideCanBeMoreRestrictive() {
        Logger.level(LogLevel.DEBUG);
        Logger.level(CACHE_PKG, LogLevel.WARN);

        assertFalse(Logger.enabled(LogLevel.DEBUG, CACHE_CLASS));
        assertTrue(Logger.enabled(LogLevel.WARN, CACHE_CLASS));
    }

    @Test
    void offSilencesEverything() {
        Logger.level(LogLevel.OFF);

        assertFalse(Logger.enabled(LogLevel.FATAL, CACHE_CLASS));
        assertFalse(Logger.enabled(LogLevel.DEBUG, CACHE_CLASS));
    }
}
