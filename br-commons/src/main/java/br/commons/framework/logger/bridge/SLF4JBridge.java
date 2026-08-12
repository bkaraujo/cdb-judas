package br.commons.framework.logger.bridge;

import br.commons.Logger;
import br.commons.framework.logger.LogLevel;
import br.commons.tools.Strings;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.function.Supplier;

@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class SLF4JBridge extends LegacyAbstractLogger {

    public final String loggerName;
    @Override public String getName() { return loggerName; }
    @Override protected String getFullyQualifiedCallerName() { return loggerName; }

    @Override
    protected void handleNormalizedLoggingCall(Level level, @Nullable Marker marker, String messagePattern, @Nullable Object @Nullable [] arguments, @Nullable Throwable throwable) {
        // Check if this level is enabled for the logger name (respects package filters)
        val judiaLevel = toLogLevel(level);
        if (!isEnabled(judiaLevel)) return;

        val messageSupplier = new LogMessageSupplier(messagePattern, arguments, marker, throwable);

        // Falha ao logar nunca pode subir para quem logou: aqui o chamador é biblioteca de terceiro,
        // muitas vezes um <clinit> (io.netty.util.NetUtil), onde uma exceção vira
        // ExceptionInInitializerError e derruba a aplicação inteira.
        try {
            dispatch(level, messageSupplier);
        } catch (RuntimeException e) {
            // Último recurso: o canal de log é justamente o que falhou, então vai direto no stderr.
            System.err.printf("Falha ao encaminhar log de '%s': %s%n", loggerName, e);
        }
    }

    /** Use WithCaller methods to preserve caller class name. */
    private void dispatch(Level level, Supplier<String> message) {
        switch (level) {
            case TRACE -> Logger.traceWithCaller(() -> loggerName, message);
            case DEBUG -> Logger.debugWithCaller(() -> loggerName, message);
            case INFO -> Logger.infoWithCaller(() -> loggerName, message);
            case WARN -> Logger.warnWithCaller(() -> loggerName, message);
            case ERROR -> Logger.errorWithCaller(() -> loggerName, message);
        }
    }

    private static LogLevel toLogLevel(Level level) {
        return switch (level) {
            case TRACE -> LogLevel.TRACE;
            case DEBUG -> LogLevel.DEBUG;
            case INFO -> LogLevel.INFO;
            case WARN -> LogLevel.WARN;
            case ERROR -> LogLevel.ERROR;
        };
    }

    @Override public boolean isTraceEnabled() { return isEnabled(LogLevel.TRACE); }
    @Override public boolean isDebugEnabled() { return isEnabled(LogLevel.DEBUG); }
    @Override public boolean isInfoEnabled() { return isEnabled(LogLevel.INFO); }
    @Override public boolean isWarnEnabled() { return isEnabled(LogLevel.WARN); }
    @Override public boolean isErrorEnabled() { return isEnabled(LogLevel.ERROR); }

    private boolean isEnabled(LogLevel candidate) {
        // Check effective level for this logger name (respects package filters)
        val effectiveLevel = Logger.level(loggerName);
        return candidate.ordinal >= effectiveLevel.ordinal;
    }

    @NullMarked
    private record LogMessageSupplier(
            String messagePattern,
            @SuppressWarnings("ArrayRecordComponent")
            Object @Nullable [] arguments,
            @Nullable Marker marker,
            @Nullable Throwable throwable
    ) implements Supplier<String> {
            @Override
            public String get() {
                var message = MessageFormatter.arrayFormat(messagePattern, arguments).getMessage();

                // Add throwable stack trace if present
                if (throwable != null) {
                    val sb = new StringBuilder(message);
                    sb.append(Strings.LINE_SEPARATOR).append(throwable.getClass().getName());
                    if (throwable.getMessage() != null) {
                        sb.append(": ").append(throwable.getMessage());
                    }

                    sb.append(Strings.LINE_SEPARATOR);
                    for (val element : throwable.getStackTrace()) {
                        sb.append("  at ").append(element).append(Strings.LINE_SEPARATOR);
                    }

                    // Add cause if present
                    val cause = throwable.getCause();
                    if (cause != null) {
                        sb.append("Caused by: ").append(cause.getClass().getName());
                        if (cause.getMessage() != null) {
                            sb.append(": ").append(cause.getMessage());
                        }

                        sb.append(Strings.LINE_SEPARATOR);
                        for (val element : cause.getStackTrace()) {
                            sb.append("  at ").append(element).append(Strings.LINE_SEPARATOR);
                        }
                    }

                    message = sb.toString();
                }

                return message;
            }
        }
}
