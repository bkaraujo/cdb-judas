package br.commons.framework.logger.channel;

import br.commons.Platform;
import br.commons.framework.logger.LogChannel;
import br.commons.tools.Threads;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@NullMarked
public final class RollingFileChannel implements LogChannel {
    private final Queue<String> messages = new ConcurrentLinkedQueue<>();

    public RollingFileChannel() {
        val runnable = new RollingFileWriter(messages, Platform.fileSystem().jvmFS());
        Threads.virtual(runnable);
    }

    @Override
    public void write(String message) {
        messages.add(message);
    }

}
