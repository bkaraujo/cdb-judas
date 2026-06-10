package br.commons.framework.logger.channel;

import br.commons.RT;
import br.commons.tools.Threads;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Queue;

import static br.commons.tools.chrono.Dates.MILLIS_IN_DAY;

@NullMarked
final class RollingFileWriter implements Runnable {
    private final Path rootDirectory;
    private final Queue<String> messages;

    private @org.jspecify.annotations.Nullable BufferedWriter currentWriter;
    private long currentDayCached;

    public RollingFileWriter(Queue<String> messages, Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.messages = messages;
    }

    @Override
    public void run() {
        // Inicializa o primeiro writer
        rotateWriter();

        while (RT.running) {
            long nowDay = System.currentTimeMillis() / MILLIS_IN_DAY;
            if (nowDay != currentDayCached) { rotateWriter(); }

            if (messages.isEmpty()) {
                Threads.sleep(100);
                continue;
            }

            String message;
            val writer = currentWriter;
            if (writer != null) {
                while ((message = messages.poll()) != null) {
                    write(writer, message);
                }
                flush(writer);
            }
        }

        close(currentWriter);
    }

    private void rotateWriter() {
        try {
            // Fecha o anterior se existir
            val writer = currentWriter;
            if (writer != null) {
                writer.flush();
                writer.close();
            }

            // Atualiza o cache do dia atual
            long nowMillis = System.currentTimeMillis();
            currentDayCached = nowMillis / MILLIS_IN_DAY;

            // Cria o novo nome de arquivo: ex: "log-2026-02-06.txt"
            // LocalDate.ofEpochDay é eficiente aqui
            val dateStr = LocalDate.ofEpochDay(currentDayCached).toString();
            val fileName = dateStr + ".log";
            val filePath = rootDirectory.resolve(fileName);

            // Garante que o diretório pai exista
            if (!Files.exists(rootDirectory)) {
                Files.createDirectories(rootDirectory);
            }

            // Cria novo writer com buffer e append
            currentWriter = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE
            );

        } catch (IOException exception) {
            System.err.println(exception);
            System.exit(15);
        }
    }

    private void write(BufferedWriter writer, String message) {
        try {
            Ansi.writeStripped(writer, message);
        } catch (IOException exception) {
            System.err.println(exception);
            System.exit(16);
        }
    }

    private void flush(BufferedWriter writer) {
        try { writer.flush(); }
        catch (IOException e) {
            // Ignorado durante encerramento ou rotação
        }
    }

    private void close(@org.jspecify.annotations.Nullable BufferedWriter writer) {
        if (writer == null) return;

        try { writer.close(); }
        catch (IOException e) {
            // Ignorado durante encerramento ou rotação
        }
    }
}
