package br.commons.framework.logger;

import br.commons.Logger;
import br.commons.RT;
import br.commons.framework.logger.channel.ConsoleChannel;
import logsample.alpha.Alpha;
import logsample.beta.Beta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garante que overrides de nível por pacote ignoram o nível do rootlogger — tanto para
 * ficar mais verboso quanto mais restritivo que o global.
 */
class LoggerPackageLevelTest {

    private static final String ALPHA_PKG = "logsample.alpha";

    // Frames de infraestrutura excluídos da resolução de caller (espelha Application.main).
    // Necessário em teste unitário, onde o bootstrap da aplicação não roda.
    private static final List<String> INFRA_EXCLUSIONS = List.of(
            "br.commons.framework.logger.",
            "br.commons.Logger",
            "br.commons.tools.Meta"
    );

    private final List<String> captured = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        for (String pkg : INFRA_EXCLUSIONS) {
            if (!RT.packages.contains(pkg)) {
                RT.packages.add(pkg);
            }
        }
        captured.clear();
        Logger.channel(captured::add);
        Logger.clearPackageFilters();
        Logger.level(LogLevel.INFO);
    }

    @AfterEach
    void tearDown() {
        Logger.clearPackageFilters();
        Logger.level(LogLevel.INFO);
        Logger.channel(new ConsoleChannel());
    }

    private boolean captured(String marker) {
        return captured.stream().anyMatch(line -> line.contains(marker));
    }

    @Test
    void packageMoreVerboseThanRoot_emits() {
        Logger.level(LogLevel.WARN);             // root silencia DEBUG
        Logger.level(ALPHA_PKG, LogLevel.DEBUG); // pacote reabre DEBUG

        Alpha.debug("alpha-debug");
        Beta.debug("beta-debug");

        assertTrue(captured("alpha-debug"), "pacote com DEBUG deve emitir, ignorando o root WARN");
        assertFalse(captured("beta-debug"), "pacote sem override deve seguir o root WARN");
    }

    @Test
    void packageMoreRestrictiveThanRoot_suppresses() {
        Logger.level(LogLevel.DEBUG);           // root permite DEBUG
        Logger.level(ALPHA_PKG, LogLevel.WARN); // pacote silencia abaixo de WARN

        Alpha.debug("alpha-debug");
        Alpha.warn("alpha-warn");
        Beta.debug("beta-debug");

        assertFalse(captured("alpha-debug"), "pacote com WARN deve suprimir DEBUG, ignorando o root DEBUG");
        assertTrue(captured("alpha-warn"), "WARN do pacote deve emitir");
        assertTrue(captured("beta-debug"), "pacote sem override deve seguir o root DEBUG");
    }

    @Test
    void rootOff_silencesEverything_butPackageCanReenable() {
        Logger.level(LogLevel.OFF);

        Beta.info("beta-info");
        assertFalse(captured("beta-info"), "root OFF deve silenciar tudo");

        Logger.level(ALPHA_PKG, LogLevel.INFO); // pacote reabre apesar do root OFF
        Alpha.info("alpha-info");
        assertTrue(captured("alpha-info"), "pacote com INFO deve emitir mesmo com root OFF");
    }
}
