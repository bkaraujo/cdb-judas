package br.commons.debug;

import br.commons.Logger;
import br.commons.chrono.Time;
import br.commons.tools.Meta;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public abstract class Execution {
    private Execution() {}

    public static <T> T nanos(Supplier<T> supplier) { return time(supplier, Time::nanos, "ns"); }
    public static <T> T millis(Supplier<T> supplier) { return time(supplier, Time::millis, "ms"); }
    public static <T> T time(Supplier<T> supplier, Supplier<Long> clock, String scale) {
        val start = clock.get();
        try {
            return supplier.get();
        } finally {
            Logger.debug("%s time: %s %s", Meta.method(supplier), clock.get()- start, scale);
        }
    }

    public static void nanos(Runnable runnable) { time(runnable, Time::nanos, "ns"); }
    public static void millis(Runnable runnable) { time(runnable, Time::millis, "ms"); }
    public static void time(Runnable runnable, Supplier<Long> clock, String scale) {
        val start = clock.get();
        try {
            runnable.run();
        } finally {
            Logger.debug("%s time: %s %s", Meta.method(runnable), clock.get()- start, scale);
        }
    }
}
