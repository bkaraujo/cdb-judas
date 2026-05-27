package br.commons.framework.logger.forwarder;

import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.LogRecord;
import br.commons.tools.chrono.Time;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public sealed class ErrorForwarder extends FatalForwarder permits WarnForwarder {

    @Override
    public final void error(Supplier<String> messageSupplier) {
        forward(new LogRecord(
                Time.millis(),
                LogLevel.ERROR,
                messageSupplier
        ));
    }

}
