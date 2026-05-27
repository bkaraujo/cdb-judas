package br.commons.tools.chrono;

import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@NullMarked
public abstract class Time {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static long millis() {
        return System.currentTimeMillis();
    }

    public static long nanos() {
        return System.nanoTime();
    }

    public static double seconds() {
        return System.currentTimeMillis() * 1e-3d;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    public static String yyyyMMdd() {
        return now().format(DATE_FORMATTER);
    }
}
