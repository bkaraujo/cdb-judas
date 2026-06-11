package br.commons.framework.logger.forwarder;

import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.LogRecord;
import br.commons.chrono.Time;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public final class VerboseForwarder extends TraceForwarder {

    @Override
    public void verbose(Supplier<String> messageSupplier) {
        forward(new LogRecord(
                Time.millis(),
                LogLevel.VERBOSE,
                messageSupplier
        ));
    }

}
