package br.commons.tools.platform.provider.agnostic;

import br.commons.tools.platform.Terminal;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.awt.*;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@NullMarked
public abstract class AbstractTerminal implements Terminal {

    // ========================================================================
    // Output
    // ========================================================================

    private static final byte[] CSI = {0x1b, '['};
    private static final byte SEMICOLON = ';';
    private static final byte CMD_H = 'H';

    private byte[] renderBuffer = new byte[4096];
    private int renderPos;

    @Override
    @SuppressWarnings("EmptyCatch")
    public final void render(MemorySegment buffer, Dimension dimension, int[] indices, int count) {
        if (count == 0) return;

        val width = dimension.width;
        val estimatedSize = count * 12;
        if (renderBuffer.length < estimatedSize) {
            renderBuffer = new byte[estimatedSize];
        }

        renderPos = 0;

        var prevIndex = -2;
        for (int i = 0; i < count; i++) {
            val index = indices[i];
            val codePoint = buffer.get(ValueLayout.JAVA_INT, index * 4L);

            // Skip cursor move if consecutive cell on same row
            if (index != prevIndex + 1 || index % width == 0) {
                val x = index % width + 1;
                val y = index / width + 1;
                writeCsi();
                writeInt(y);
                writeByte(SEMICOLON);
                writeInt(x);
                writeByte(CMD_H);
            }
            writeCodePoint(codePoint);
            prevIndex = index;
        }

        try {
            System.out.write(renderBuffer, 0, renderPos);
        } catch (Exception ignored) {}
    }

    private void writeCsi() {
        renderBuffer[renderPos++] = CSI[0];
        renderBuffer[renderPos++] = CSI[1];
    }

    private void writeByte(byte b) {
        renderBuffer[renderPos++] = b;
    }

    private void writeInt(int value) {
        if (value >= 100) {
            renderBuffer[renderPos++] = (byte) ('0' + value / 100);
            value %= 100;
            renderBuffer[renderPos++] = (byte) ('0' + value / 10);
            renderBuffer[renderPos++] = (byte) ('0' + value % 10);
        } else if (value >= 10) {
            renderBuffer[renderPos++] = (byte) ('0' + value / 10);
            renderBuffer[renderPos++] = (byte) ('0' + value % 10);
        } else {
            renderBuffer[renderPos++] = (byte) ('0' + value);
        }
    }

    private void writeCodePoint(int cp) {
        if (cp < 0x80) {
            renderBuffer[renderPos++] = (byte) cp;
        } else if (cp < 0x800) {
            renderBuffer[renderPos++] = (byte) (0xC0 | (cp >> 6));
            renderBuffer[renderPos++] = (byte) (0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            renderBuffer[renderPos++] = (byte) (0xE0 | (cp >> 12));
            renderBuffer[renderPos++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            renderBuffer[renderPos++] = (byte) (0x80 | (cp & 0x3F));
        } else {
            renderBuffer[renderPos++] = (byte) (0xF0 | (cp >> 18));
            renderBuffer[renderPos++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            renderBuffer[renderPos++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            renderBuffer[renderPos++] = (byte) (0x80 | (cp & 0x3F));
        }
    }
}
