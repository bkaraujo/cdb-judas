package br.commons.framework.logger.channel;

import br.commons.framework.logger.LogChannel;
import br.commons.tools.Threads;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@NullMarked
public final class FileChannel implements LogChannel {

    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    public FileChannel(Path path) {
        Threads.daemon("FileChannel[" + path.getFileName() + "]", () -> run(path));
    }

    @Override
    public void write(String message) {
        queue.offer(message);
    }

    private void run(Path path) {
        try {
            val writer = Files.newBufferedWriter(
                    path,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            try {
                while (true) {
                    drain(writer);
                    Threads.sleep(100);
                }
            } finally {
                try {
                    drain(writer);
                    writer.close();
                } catch (IOException e) {
                    // Ignorado durante encerramento
                }
            }
        } catch (IOException e) {
            System.err.println("[FileChannel] Falha ao abrir " + path + ": " + e.getMessage());
        }
    }

    private void drain(BufferedWriter writer) throws IOException {
        String message;
        while ((message = queue.poll()) != null) {
            Ansi.writeStripped(writer, message);
        }
        writer.flush();
    }
}
