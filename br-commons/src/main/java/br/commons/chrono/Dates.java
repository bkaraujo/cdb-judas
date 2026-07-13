package br.commons.chrono;

import org.jspecify.annotations.NullMarked;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@NullMarked
public abstract class Dates {
    private Dates(){}

    // Constante para converter millis em dias (24 * 60 * 60 * 1000)
    public static final long MILLIS_IN_DAY = 86_400_000L;

    public static final Pattern PATTERN_DDMMYYYY = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
    public static final DateTimeFormatter FORMATTER_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final Map<Locale, Map<String, Integer>> LOCALIZED = Map.of(
            Locale.of("pt", "BR"), Map.ofEntries(
                    Map.entry("JANEIRO", 1), Map.entry("FEVEREIRO", 2), Map.entry("MARÇO", 3),
                    Map.entry("ABRIL", 4), Map.entry("MAIO", 5), Map.entry("JUNHO", 6),
                    Map.entry("JULHO", 7), Map.entry("AGOSTO", 8), Map.entry("SETEMBRO", 9),
                    Map.entry("OUTUBRO", 10), Map.entry("NOVEMBRO", 11), Map.entry("DEZEMBRO", 12)
            ),

            Locale.of("en", "US"), Map.ofEntries(
                    Map.entry("JANUARY", 1), Map.entry("FEBRUARY", 2), Map.entry("MARCH", 3),
                    Map.entry("APRIL", 4), Map.entry("MAY", 5), Map.entry("JUNE", 6),
                    Map.entry("JULLY", 7), Map.entry("AUGUST", 8), Map.entry("SEPTEMBER", 9),
                    Map.entry("OCTOBER", 10), Map.entry("NOVEMBER", 11), Map.entry("DECEMBER", 12)
            )
    );

    public static final Map<String, Integer> MONTHS_PTBR = LOCALIZED.get(Locale.of("pt", "BR"));
    public static final Map<String, Integer> MONTHS_ENUS = LOCALIZED.get(Locale.of("en", "US"));
}
