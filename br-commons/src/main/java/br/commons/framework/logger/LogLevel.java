package br.commons.framework.logger;

import br.commons.platform.terminal.TerminalColor;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

@NullMarked
public enum LogLevel {
    OFF    ("OFF", TerminalColor.RESET),
    VERBOSE("VERBOSE", TerminalColor.PURPLE_BOLD),
    TRACE  ("TRACE  ", TerminalColor.PURPLE_BOLD),
    DEBUG  ("DEBUG  ", TerminalColor.BLUE_BOLD),
    INFO   ("INFO   ", TerminalColor.GREEN_BOLD),
    WARN   ("WARN   ", TerminalColor.YELLOW_BOLD),
    ERROR  ("ERROR  ", TerminalColor.RED_BOLD),
    FATAL  ("FATAL  ", TerminalColor.RED_BOLD);

    public final TerminalColor color;
    public final String colored;
    public final int ordinal;

    @SuppressWarnings("EnumOrdinal")
    LogLevel(String message, TerminalColor desired) {
        this.color = desired;
        colored = "%s%s".formatted(desired.color, message);
        this.ordinal = this.ordinal();
    }


    public static LogLevel fromSlf4jLevel(String level) {
        return switch (Strings.upper(level)) {
            case "TRACE" -> TRACE;
            case "DEBUG" -> DEBUG;
            case "WARN", "WARNING" -> WARN;
            case "ERROR" -> ERROR;
            case "OFF" -> OFF;
            default -> INFO;
        };
    }
}
