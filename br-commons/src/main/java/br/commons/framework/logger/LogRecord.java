package br.commons.framework.logger;

import br.commons.tools.meta.StackFrame;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

@NullMarked
public record LogRecord(
        long timestamp,
        LogLevel level,
        String threadName,
        Supplier<String> messageSupplier,
        @Nullable StackFrame callerClass) {

    public LogRecord(long timestamp, LogLevel level, Supplier<String> messageSupplier) {
        this(timestamp, level, Thread.currentThread().getName(), messageSupplier, null);
    }

    public String userMessage() {
        return messageSupplier.get();
    }

}
