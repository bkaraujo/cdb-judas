package br.commons.framework.logger.channel;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Escreve uma linha removendo sequências ANSI (CSI {@code ESC[...m}) — lógica compartilhada
 * pelos channels de arquivo ({@link FileChannel} e o rolling writer), que não devem gravar cor.
 */
@NullMarked
final class Ansi {

    /** ESC (0x1B): início de toda sequência ANSI. */
    private static final char ESCAPE = 0x1B;

    private Ansi() {
    }

    static void writeStripped(BufferedWriter writer, String message) throws IOException {
        val length = message.length();
        int lastWritePos = 0;
        int currentPos = 0;

        while (currentPos < length) {
            if (message.charAt(currentPos) != ESCAPE) {
                currentPos++;
                continue;
            }
            val len = currentPos - lastWritePos;
            if (len > 0) {
                writer.write(message, lastWritePos, len);
            }
            val end = sequenceEnd(message, currentPos, length);
            if (end < 0) {
                currentPos++;
                continue;
            }
            currentPos = end;
            lastWritePos = currentPos;
        }

        val tailLen = length - lastWritePos;
        if (tailLen > 0) {
            writer.write(message, lastWritePos, tailLen);
        }
        writer.newLine();
    }

    /** Índice logo após {@code ESC[...m}, ou {@code -1} se não for um CSI (ESC isolado). */
    private static int sequenceEnd(String message, int escPos, int length) {
        int end = escPos + 1;
        if (end >= length || message.charAt(end) != '[') {
            return -1;
        }
        end++;
        while (end < length && message.charAt(end++) != 'm') {
            /* avança até o terminador 'm' */
        }
        return end;
    }
}
