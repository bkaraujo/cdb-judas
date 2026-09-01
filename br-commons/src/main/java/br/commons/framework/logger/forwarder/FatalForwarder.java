package br.commons.framework.logger.forwarder;

import br.commons.Logger;
import br.commons.framework.logger.LogForwarder;
import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.LogRecord;
import br.commons.framework.logger.MDC;
import br.commons.platform.terminal.TerminalColor;
import br.commons.tools.Meta;
import br.commons.tools.meta.StackFrame;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Calendar;
import java.util.function.Supplier;

@NullMarked
public sealed class FatalForwarder implements LogForwarder permits ErrorForwarder {

    private static final ThreadLocal<Calendar> CALENDAR = ThreadLocal.withInitial(Calendar::getInstance);
    private static final ThreadLocal<StringBuilder> BUILDER = ThreadLocal.withInitial(
            () -> new StringBuilder(1024)
    );

    // Evita divisão/módulo para conversão de dígitos
    private static final char[] HORIZONTAL = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'
    };

    // Evita divisão/módulo para conversão de dígitos
    private static final char[] VERTICAL = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    @Override
    public final void fatal(Supplier<String> messageSupplier) {
        forward(new LogRecord(
                System.currentTimeMillis(),
                LogLevel.FATAL,
                messageSupplier
        ));

        Meta.exit(99);
    }

    protected final void forward(LogRecord record) {
        val frame = resolveStackFrame(record);
        val builder = BUILDER.get();
        builder.setLength(0);

        formatMessage(builder, record, frame);
        for (val channel : Logger.channels()) {
            channel.write(builder.toString());
        }
    }

    private StackFrame resolveStackFrame(LogRecord record) {
        val external = Logger.externalCaller();
        if (external != null) { return new StackFrame(external, "", 0); }

        val fromRecord = record.callerClass();
        if (fromRecord != null) { return fromRecord; }

        return Meta.stackFrame(0);
    }

    private void formatMessage(StringBuilder sb, LogRecord record, StackFrame frame) {
        val level = record.level();

        // ##########################################################
        // Current Timestamp
        // ##########################################################
        appendTimestamp(sb, record.timestamp());
        sb.append(' ');
        // ##########################################################
        // Current Log Level
        // ##########################################################
        sb.append(level.colored).append(TerminalColor.RESET.color).append(" [");
        sb.append(level.color).append(record.threadName()).append(TerminalColor.RESET.color);
        sb.append("] ");
        // ##########################################################
        // Actual Logging caller
        // ##########################################################
        sb.append(level.color)
          .append(frame.className()).append(':').append(frame.lineNumber())
          .append(TerminalColor.RESET.color)
          .append(' ');
        // ##########################################################
        // Diagnostic Context Messages
        // ##########################################################
        sb.
                append(MDC.prefix())
                .append(' ');
        // ##########################################################
        // User message
        // ##########################################################
        sb.append(record.userMessage());
    }

    private void appendTimestamp(StringBuilder sb, long timestamp) {
        val cal = CALENDAR.get();
        cal.setTimeInMillis(timestamp);

        // yyyy-MM-dd
        val year = cal.get(Calendar.YEAR);
        val month = cal.get(Calendar.MONTH) + 1;
        val day = cal.get(Calendar.DAY_OF_MONTH);

        sb
                .append((char) ('0' + year / 1000)).append((char) ('0' + (year / 100) % 10)).append(HORIZONTAL[year % 100]).append(VERTICAL[year % 100])
                .append('-').append(HORIZONTAL[month]).append(VERTICAL[month])
                .append('-').append(HORIZONTAL[day]).append(VERTICAL[day]);

        // HH:mm:ss
        val hour = cal.get(Calendar.HOUR_OF_DAY);
        val minute = cal.get(Calendar.MINUTE);
        val second = cal.get(Calendar.SECOND);

        sb
                .append(' ').append(HORIZONTAL[hour]).append(VERTICAL[hour])
                .append(':').append(HORIZONTAL[minute]).append(VERTICAL[minute])
                .append(':').append(HORIZONTAL[second]).append(VERTICAL[second])
                ;

        // ,SSS
        val millis = cal.get(Calendar.MILLISECOND);

        sb
                .append(',')
                .append((char) ('0' + millis / 100))
                .append(HORIZONTAL[millis % 100]).append(VERTICAL[millis % 100]);
    }

}
