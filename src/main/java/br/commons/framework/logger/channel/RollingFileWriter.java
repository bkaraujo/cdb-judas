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
            val length = message.length();
            val ANSI_ESCAPE = '\u001B';
            var lastWritePos = 0;
            var currentPos = 0;

            // Cache local da referência para evitar getfield repetido no loop (micro-otimização)
            // Em Java 25 isso é menos crítico, mas boa prática em hotpath.

            while (currentPos < length) {
                val c = message.charAt(currentPos);
                if (c == ANSI_ESCAPE) {
                    // 1. Flush do conteúdo válido anterior
                    val len = currentPos - lastWritePos;
                    if (len > 0) {
                        writer.write(message, lastWritePos, len);
                    }

                    // 2. Scan do código ANSI
                    var endAnsi = currentPos + 1;

                    // Verifica se é um CSI (Control Sequence Introducer) [
                    if (endAnsi < length && message.charAt(endAnsi) == '[') {
                        endAnsi++; // Pula o '['

                        // Avança até encontrar 'm' ou acabar a string
                        // Otimização: Loop forçado para frente sem verificações extras complexas
                        while (endAnsi < length) {
                            if (message.charAt(endAnsi) == 'm') {
                                endAnsi++; // Inclui o 'm' no pulo
                                break;
                            }

                            endAnsi++;
                        }

                        // Define o novo ponto de escrita
                        currentPos = endAnsi;
                        lastWritePos = currentPos;
                        continue; // Pula o incremento final pois já atualizamos currentPos
                    }
                    // Se não for CSI (ex: ESC isolado), tratamos como char normal na próxima iteração
                }

                currentPos++;
            }

            // 3. Flush final (Tail)
            val tailLen = length - lastWritePos;
            if (tailLen > 0) { writer.write(message, lastWritePos, tailLen); }

            writer.newLine();
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
