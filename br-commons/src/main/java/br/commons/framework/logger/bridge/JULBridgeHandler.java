package br.commons.framework.logger.bridge;

import br.commons.Logger;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.text.MessageFormat;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

@NullMarked
public class JULBridgeHandler extends Handler {

    private static final String UNKNOWN_LOGGER_NAME = "unknown.jul.logger";

    private static final int TRACE_LEVEL_THRESHOLD = Level.FINEST.intValue();
    private static final int DEBUG_LEVEL_THRESHOLD = Level.FINE.intValue();
    private static final int INFO_LEVEL_THRESHOLD = Level.INFO.intValue();
    private static final int WARN_LEVEL_THRESHOLD = Level.WARNING.intValue();

    public static void install() {
        val rootLogger = getRootLogger();
        val handlers = rootLogger.getHandlers();
        for (val handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        LogManager.getLogManager().getLogger(Strings.EMPTY).addHandler(new JULBridgeHandler());
    }

    private static java.util.logging.Logger getRootLogger() {
        return LogManager.getLogManager().getLogger(Strings.EMPTY);
    }

    @Override public void close() {}
    @Override public void flush() {}

    @Override public void publish(LogRecord record) {
        if (record == null) return;
        if (record.getMessage() == null) record.setMessage(Strings.EMPTY);

        val callerClass = new CallerSupplier(record);
        val message = new LogMessageSupplier(record);

        val julLevelValue = record.getLevel().intValue();
        if (julLevelValue <= TRACE_LEVEL_THRESHOLD) { Logger.traceWithCaller(callerClass, message); }
        else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) { Logger.debugWithCaller(callerClass, message); }
        else if (julLevelValue <= INFO_LEVEL_THRESHOLD) { Logger.infoWithCaller(callerClass, message); }
        else if (julLevelValue <= WARN_LEVEL_THRESHOLD) { Logger.warnWithCaller(callerClass, message); }
        else { Logger.errorWithCaller(callerClass, message); }
    }

    @NullMarked
    private record CallerSupplier(LogRecord record) implements Supplier<String> {
        @Override
        public String get() {
            var callerClass = record.getSourceClassName();
            if (callerClass == null || callerClass.isEmpty()) {
                callerClass = record.getLoggerName();
                if (callerClass == null || callerClass.isEmpty()) {
                    callerClass = UNKNOWN_LOGGER_NAME;
                }
            }

            return callerClass;
        }
    }

    @NullMarked
    private record LogMessageSupplier(LogRecord record) implements Supplier<String> {
        @Override
        public String get() {
            try {
                var message = record.getMessage();
                val bundle = record.getResourceBundle();
                if (bundle != null) {
                    message = bundle.getString(message);
                }

                val params = record.getParameters();
                if (params != null && params.length > 0) {
                    message = MessageFormat.format(message, params);
                }

                if (message == null) {
                    message = Strings.EMPTY;
                }

                val thrown = record.getThrown();
                if (thrown != null) {
                    message = message + "\nException: " + thrown.getClass().getName() + ": " + thrown.getMessage();
                }

                return message;
            } catch (Exception ex) {
                Logger.error("Failed to wrap message: %s", ex.toString());
                return Strings.EMPTY;
            }
        }
    }
}
