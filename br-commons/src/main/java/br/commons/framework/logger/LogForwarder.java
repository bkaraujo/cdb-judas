package br.commons.framework.logger;

import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public interface LogForwarder {

    default void verbose(Supplier<String> messageSupplier) {}
    default void trace(Supplier<String> messageSupplier) {}
    default void debug(Supplier<String> messageSupplier) {}
    default void info(Supplier<String> messageSupplier) {}
    default void warn(Supplier<String> messageSupplier) {}
    default void error(Supplier<String> messageSupplier) {}
    default void fatal(Supplier<String> messageSupplier) {}

}
