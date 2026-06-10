package br.commons;

import br.commons.framework.logger.LogChannel;
import br.commons.framework.logger.LogFilter;
import br.commons.framework.logger.LogForwarder;
import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.bridge.JULBridgeHandler;
import br.commons.framework.logger.channel.ConsoleChannel;
import br.commons.framework.logger.forwarder.*;
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

    private static LogForwarder forwarder = new InfoForwarder();
    private static final Map<LogLevel, LogForwarder> forwarders = Map.of(
            LogLevel.OFF, new LogForwarder() {},
            LogLevel.VERBOSE, new VerboseForwarder(),
            LogLevel.TRACE, new TraceForwarder(),
            LogLevel.DEBUG, new DebugForwarder(),
            LogLevel.INFO, new InfoForwarder(),
            LogLevel.WARN, new WarnForwarder(),
            LogLevel.ERROR, new ErrorForwarder(),
            LogLevel.FATAL, new FatalForwarder());

    private static final LogFilter filter = new LogFilter(LogLevel.INFO);

    static {
        level(LogLevel.INFO);
        forwarder = forwarders.getOrDefault(LogLevel.INFO, new InfoForwarder());

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

    private static LogLevel level = LogLevel.INFO;
    public static LogLevel level() { return level; }
    public static void level(LogLevel desired) {
        level = desired;
        filter.level(desired);
        forwarder = forwarders.getOrDefault(desired, new InfoForwarder());
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
     * Determines if a log message should be logged based on package filters.
     *
     * @param callerClass  Caller class name
     * @param messageLevel Level of the message
     * @return true if should log, false otherwise
     */
    private static boolean shouldLog(String callerClass, LogLevel messageLevel) {
        // Fast path: se filtro global rejeita, retorna imediatamente
        if (messageLevel.ordinal < filter.level().ordinal) {
            return false;
        }

        // Se não há filtros específicos, aceita
        if (!filter.hasPackageFilters()) {
            return true;
        }

        // Slow path: verifica filtro por pacote
        val effectiveLevel = filter.getEffectiveLevel(callerClass);
        return messageLevel.ordinal >= effectiveLevel.ordinal;
    }

    /**
     * Guard compartilhado pelos métodos de log: só resolve o caller (StackWalker, caro) quando há
     * filtros por pacote ativos. Como {@code resolveCallerClass} pula os frames de infraestrutura
     * ({@link br.commons.tools.Meta#stackFrame}), chamá-lo daqui não altera o caller resolvido.
     */
    private static boolean blocked(LogLevel messageLevel) {
        return filter.hasPackageFilters() && !shouldLog(resolveCallerClass(), messageLevel);
    }

    /**
     * Helper to create lazy arguments for expensive operations.
     * Usage: Logger.debug("Value: %s", lazy(() -> expensiveOperation()))
     */
    public static Supplier<Object> lazy(Supplier<Object> supplier) {
        return supplier;
    }

    public static void verbose(String message, Object... args) {
        // OTIMIZAÇÃO: Só resolve caller se há filtros por pacote
        // O forwarder já filtra por nível (VerboseForwarder.verbose() vs default vazio)
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
