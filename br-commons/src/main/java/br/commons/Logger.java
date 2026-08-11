package br.commons;

import br.commons.framework.logger.LogChannel;
import br.commons.framework.logger.LogFilter;
import br.commons.framework.logger.LogForwarder;
import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.bridge.JULBridgeHandler;
import br.commons.framework.logger.channel.ConsoleChannel;
import br.commons.framework.logger.forwarder.VerboseForwarder;
import br.commons.tools.Meta;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NullMarked
public abstract class Logger {
    private Logger() {}

    // Forwarder único que emite qualquer nível. A decisão de emitir/ignorar é do gate
    // (blocked), baseado no nível efetivo por pacote — o forwarder nunca filtra por nível.
    private static final LogForwarder forwarder = new VerboseForwarder();

    private static final LogFilter filter = new LogFilter(LogLevel.INFO);

    static {
        level(LogLevel.INFO);

        // Precisa vir ANTES de JULBridgeHandler.install(): instalar o handler chama
        // LogManager.getLogManager(), e o bloco estático de java.util.logging.LogManager lê
        // java.util.logging.manager UMA vez, na inicialização da classe. Quem inicializa o JUL
        // primeiro decide a implementação para sempre — se sobrar a default, o LogManager do
        // JBoss (exigido pelo Quarkus) não entra mais e o JBossLoggerFinder só consegue reclamar:
        // "The LogManager accessed before the java.util.logging.manager system property was set".
        installJBossLogManager();

        JULBridgeHandler.install();

        // Configure JBoss Logging (used by Hibernate) to use SLF4J
        System.setProperty("org.jboss.logging.provider", "slf4j");

        // Configuração global
        val globalLevel = System.getProperty("judia.log.level");
        if (globalLevel != null) {
            try {
                level(LogLevel.valueOf(Strings.upper(globalLevel)));
            } catch (IllegalArgumentException e) {
                warn("Invalid log level in judia.log.level: %s", globalLevel);
            }
        }

        // Configuração por pacote
        System.getProperties().forEach((key, value) -> {
            if (key instanceof String k && k.startsWith("judia.log.level.")) {
                val packageName = k.substring("judia.log.level.".length());
                try {
                    val pkgLevel = LogLevel.valueOf(Strings.upper(value));
                    level(packageName, pkgLevel);
                    info("Log level for package %s set to %s", packageName, pkgLevel);
                } catch (IllegalArgumentException e) {
                    warn("Invalid log level for package %s: %s", packageName, value);
                }
            }
        });
    }

    /** Propriedade que o {@code java.util.logging.LogManager} lê para escolher a implementação. */
    private static final String JUL_MANAGER_PROPERTY = "java.util.logging.manager";

    /** Nome literal de propósito: referenciar a classe aqui carregaria — e inicializaria — o JUL. */
    private static final String JBOSS_LOG_MANAGER = "org.jboss.logmanager.LogManager";

    /**
     * Elege o LogManager do JBoss como implementação do JUL, quando ele está no classpath e ninguém
     * escolheu outra na linha de comando ({@code -Djava.util.logging.manager=...}, como faz o
     * {@code quarkus:dev} e o fork do surefire).
     *
     * <p>{@code Class.forName} com {@code initialize=false} apenas carrega: inicializar a subclasse
     * inicializaria {@code java.util.logging.LogManager} (a superclasse) e o JUL travaria na
     * implementação default — exatamente o que este método existe para evitar. Sem a checagem de
     * presença, apontar a propriedade para uma classe ausente faria o JUL falhar em carregá-la.
     */
    private static void installJBossLogManager() {
        if (System.getProperty(JUL_MANAGER_PROPERTY) != null) return;

        try {
            Class.forName(JBOSS_LOG_MANAGER, false, Logger.class.getClassLoader());
            System.setProperty(JUL_MANAGER_PROPERTY, JBOSS_LOG_MANAGER);
        } catch (ClassNotFoundException | LinkageError e) {
            // Sem jboss-logmanager no classpath: o JUL segue com a implementação default.
        }
    }

    private static LogLevel level = LogLevel.INFO;
    public static LogLevel level() { return level; }
    public static void level(LogLevel desired) {
        level = desired;
        filter.level(desired);
    }

    private static final List<LogChannel> channels = new CopyOnWriteArrayList<>(List.of(new ConsoleChannel()));
    public static List<LogChannel> channels() { return channels; }
    public static void channel(LogChannel desired) { channels.clear(); channels.add(desired); }
    public static void addChannel(LogChannel channel) { channels.add(channel); }

    // Package-level filtering methods

    /**
     * Configura nível de log para um pacote específico.
     *
     * @param packageName Nome completo do pacote (ex: "br.framework.mcp")
     * @param desired     Nível de log para o pacote
     */
    public static void level(String packageName, LogLevel desired) {
        filter.setPackageLevel(packageName, desired);
    }

    /**
     * Retorna o nível efetivo para um pacote, considerando herança hierárquica.
     *
     * @param packageName Nome completo do pacote
     * @return Nível efetivo para o pacote
     */
    public static LogLevel level(String packageName) {
        return filter.getEffectiveLevel(packageName);
    }

    /**
     * Limpa todas as configurações de nível por pacote.
     */
    public static void clearPackageFilters() {
        filter.clearPackageFilters();
    }

    /**
     * Remove configuração de nível para um pacote específico.
     *
     * @param packageName Nome do pacote a remover
     */
    public static void removePackageLevel(String packageName) {
        filter.removePackageLevel(packageName);
    }

    // Helper methods

    /**
     * Resolve the caller class name.
     * First checks externalCaller (for SLF4J bridge), then uses StackWalker.
     *
     * @return Caller class name
     */
    private static String resolveCallerClass() {
        val external = externalCaller.get();
        if (external != null) {
            return external;
        }
        return Meta.stackFrame(0).className();
    }

    /**
     * Guard compartilhado pelos métodos de log. O nível efetivo (override por pacote, ou o nível
     * global como fallback) é a única autoridade sobre emitir/ignorar — totalmente independente do
     * rootlogger quando há override de pacote para o caller.
     *
     * <p>Sem filtros por pacote o gate usa apenas o nível global e evita o StackWalker (caro). Com
     * filtros ativos resolve o caller e usa {@link LogFilter#getEffectiveLevel}. Como
     * {@code resolveCallerClass} pula os frames de infraestrutura
     * ({@link br.commons.tools.Meta#stackFrame}), chamá-lo daqui não altera o caller resolvido.
     */
    private static boolean blocked(LogLevel messageLevel) {
        if (!filter.hasPackageFilters()) {
            return !enabled(filter.level(), messageLevel);
        }
        return !enabled(filter.getEffectiveLevel(resolveCallerClass()), messageLevel);
    }

    /**
     * Decide se {@code messageLevel} atinge o limiar {@code threshold}. OFF (ordinal 0, o menor)
     * significa silêncio total — precisa de tratamento explícito, pois a comparação por ordinal
     * sozinha aceitaria tudo.
     */
    private static boolean enabled(LogLevel threshold, LogLevel messageLevel) {
        return threshold != LogLevel.OFF && messageLevel.ordinal >= threshold.ordinal;
    }

    /**
     * Helper to create lazy arguments for expensive operations.
     * Usage: Logger.debug("Value: %s", lazy(() -> expensiveOperation()))
     */
    public static Supplier<Object> lazy(Supplier<Object> supplier) {
        return supplier;
    }

    public static void verbose(String message, Object... args) {
        if (blocked(LogLevel.VERBOSE)) return;
        forwarder.verbose(() -> message.formatted(Meta.evaluate(args)));
    }

    public static void trace(String message, Object... args) {
        if (blocked(LogLevel.TRACE)) return;
        forwarder.trace(() -> message.formatted(Meta.evaluate(args)));
    }

    public static void debug(String message, Object... args) {
        if (blocked(LogLevel.DEBUG)) return;
        forwarder.debug(() -> message.formatted(Meta.evaluate(args)));
    }

    public static void info(String message, Object... args) {
        if (blocked(LogLevel.INFO)) return;
        forwarder.info(() -> message.formatted(Meta.evaluate(args)));
    }

    public static void warn(String message, Object... args) {
        if (blocked(LogLevel.WARN)) return;
        forwarder.warn(() -> message.formatted(Meta.evaluate(args)));
    }

    public static void error(String message, Object... args) {
        if (blocked(LogLevel.ERROR)) return;
        forwarder.error(() -> message.formatted(Meta.evaluate(args)));
    }

    // ========================================================================
    // Métodos com Supplier<String> - lazy total
    // O Supplier só é avaliado se o forwarder processar o nível
    // ========================================================================

    /**
     * Log VERBOSE com mensagem lazy.
     * O Supplier só é avaliado se o forwarder processar VERBOSE.
     *
     * @param messageSupplier supplier que produz a mensagem
     */
    public static void verbose(Supplier<String> messageSupplier) {
        if (blocked(LogLevel.VERBOSE)) return;
        forwarder.verbose(messageSupplier);
    }

    /**
     * Log TRACE com mensagem lazy.
     * O Supplier só é avaliado se o forwarder processar TRACE.
     *
     * <p>
     * Uso:
     * 
     * <pre>{@code
     * Logger.trace(() -> "Expensive: %s".formatted(expensiveOperation()));
     * }</pre>
     *
     * @param messageSupplier supplier que produz a mensagem
     */
    public static void trace(Supplier<String> messageSupplier) {
        if (blocked(LogLevel.TRACE)) return;
        forwarder.trace(messageSupplier);
    }

    /**
     * Log DEBUG com mensagem lazy.
     *
     * @param messageSupplier supplier que produz a mensagem
     */
    public static void debug(Supplier<String> messageSupplier) {
        if (blocked(LogLevel.DEBUG)) return;
        forwarder.debug(messageSupplier);
    }

    /**
     * Log INFO com mensagem lazy.
     *
     * @param messageSupplier supplier que produz a mensagem
     */
    public static void info(Supplier<String> messageSupplier) {
        if (blocked(LogLevel.INFO)) return;
        forwarder.info(() -> messageSupplier.get());
    }

    /**
     * Log WARN com mensagem lazy.
     *
     * @param messageSupplier supplier que produz a mensagem
     */
    public static void warn(Supplier<String> messageSupplier) {
        if (blocked(LogLevel.WARN)) return;
        forwarder.warn(() -> messageSupplier.get());
    }

    /**
     * Log ERROR com mensagem lazy.
     *
     * @param messageSupplier supplier que produz a mensagem
     */
    public static void error(Supplier<String> messageSupplier) {
        if (blocked(LogLevel.ERROR)) return;
        forwarder.error(() -> messageSupplier.get());
    }

    public static void fatal(String message, Object... args) {
        forwarder.fatal(() -> {
            val local = new StringBuilder(message.formatted(Meta.evaluate(args)));
            local.append("\n");

            val stackTrace = Meta.stackFrame();
            for (val entry : stackTrace.subList(2, stackTrace.size())) {
                local.append("  ").append(entry).append("\n");
            }

            return local.toString();
        });
    }

    private static final Map<LogLevel, Consumer<String>> SINKS = Map.of(
            LogLevel.VERBOSE, Logger::verbose,
            LogLevel.TRACE, Logger::trace,
            LogLevel.DEBUG, Logger::debug,
            LogLevel.INFO, Logger::info,
            LogLevel.WARN, Logger::warn,
            LogLevel.ERROR, Logger::error,
            LogLevel.FATAL, Logger::fatal);

    public static void stackTrace(LogLevel level) {
        val message = new StringBuilder("\n\n");
        val stackTrace = Meta.stackFrame();
        for (val entry : stackTrace.subList(1, stackTrace.size())) {
            message.append(entry).append("\n");
        }

        val sink = SINKS.get(level);
        if (sink == null) {
            throw new IllegalStateException("Unexpected value: " + level);
        }
        sink.accept(message.toString());
    }

    // Internal methods for SLF4J bridge to specify caller class

    private static final ThreadLocal<@Nullable String> externalCaller = new ThreadLocal<>();

    public static void verboseWithCaller(Supplier<String> c, Supplier<String> m) {
        withCaller(c, m, Logger::verbose);
    }

    public static void traceWithCaller(Supplier<String> c, Supplier<String> m) {
        withCaller(c, m, Logger::trace);
    }

    public static void debugWithCaller(Supplier<String> c, Supplier<String> m) {
        withCaller(c, m, Logger::debug);
    }

    public static void infoWithCaller(Supplier<String> c, Supplier<String> m) {
        withCaller(c, m, Logger::info);
    }

    public static void warnWithCaller(Supplier<String> c, Supplier<String> m) {
        withCaller(c, m, Logger::warn);
    }

    public static void errorWithCaller(Supplier<String> c, Supplier<String> m) {
        withCaller(c, m, Logger::error);
    }

    @Nullable
    public static String externalCaller() {
        return externalCaller.get();
    }

    private static void withCaller(Supplier<String> caller, Supplier<String> message, Consumer<String> consumer) {
        try {
            externalCaller.set(caller.get());
            consumer.accept(message.get());
        } finally {
            externalCaller.remove();
        }
    }
}
