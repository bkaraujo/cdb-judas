package br.commons.tools.chrono;

import org.jspecify.annotations.NullMarked;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

@NullMarked
public abstract class Dates {
    private Dates(){}

    // Constante para converter millis em dias (24 * 60 * 60 * 1000)
    public static final long MILLIS_IN_DAY = 86_400_000L;

    public static final Pattern PATTERN_DDMMYYYY = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
    public static final DateTimeFormatter FORMATTER_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final Map<String, Integer> ABREVIACOES = Map.ofEntries(
            Map.entry("JAN", 1),
            Map.entry("FEV", 2),
            Map.entry("MAR", 3),
            Map.entry("ABR", 4),
            Map.entry("MAI", 5),
            Map.entry("JUN", 6),
            Map.entry("JUL", 7),
            Map.entry("AGO", 8),
            Map.entry("SET", 9),
            Map.entry("OUT", 10),
            Map.entry("NOV", 11),
            Map.entry( "DEZ", 12)
    );

}
