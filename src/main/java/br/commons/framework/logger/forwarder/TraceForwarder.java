package br.commons.framework.logger.forwarder;

import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.LogRecord;
import br.commons.tools.chrono.Time;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public sealed class TraceForwarder extends DebugForwarder permits VerboseForwarder {

    @Override
    public final void trace(Supplier<String> messageSupplier) {
        forward(new LogRecord(
                Time.millis(),
                LogLevel.TRACE,
                messageSupplier
        ));
    }

}
