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
            writeStripped(writer, message);
        }
        writer.flush();
    }

    private static void writeStripped(BufferedWriter writer, String message) throws IOException {
        val length = message.length();
        val ANSI_ESCAPE = '\u001B';
        var lastWritePos = 0;
        var currentPos = 0;

        while (currentPos < length) {
            if (message.charAt(currentPos) == ANSI_ESCAPE) {
                val len = currentPos - lastWritePos;
                if (len > 0) writer.write(message, lastWritePos, len);

                var endAnsi = currentPos + 1;
                if (endAnsi < length && message.charAt(endAnsi) == '[') {
                    endAnsi++;
                    while (endAnsi < length && message.charAt(endAnsi++) != 'm') { /* avança */ }
                    currentPos = endAnsi;
                    lastWritePos = currentPos;
                    continue;
                }
            }
            currentPos++;
        }

        val tailLen = length - lastWritePos;
        if (tailLen > 0) writer.write(message, lastWritePos, tailLen);
        writer.newLine();
    }
}
