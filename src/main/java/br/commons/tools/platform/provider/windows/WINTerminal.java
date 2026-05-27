package br.commons.tools.platform.provider.windows;

import br.commons.Logger;
import br.commons.tools.platform.provider.agnostic.AbstractTerminal;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

/**
 * Windows implementation of Terminal using Java FFM.
 * Uses kernel32 for dimension and console mode control.
 */
@NullMarked
public class WINTerminal extends AbstractTerminal {

    // ========================================================================
    // Dimension
    // ========================================================================

    @Override
    public Dimension dimension() {
        try (val localArena = Arena.ofConfined()) {
            val csbi = localArena.allocate(22);

            val success = Kernel32.GetConsoleScreenBufferInfo(Kernel32.hStdout, csbi);
            if (success == 0) {
                WinAPI.printError("GetConsoleScreenBufferInfo");
                return new Dimension(0, 0);
            }

            // srWindow structure starts at offset 10
            // typedef struct { SHORT Left, Top, Right, Bottom; } SMALL_RECT;
            val left   = csbi.get(ValueLayout.JAVA_SHORT, 10);
            val top    = csbi.get(ValueLayout.JAVA_SHORT, 12);
            val right  = csbi.get(ValueLayout.JAVA_SHORT, 14);
            val bottom = csbi.get(ValueLayout.JAVA_SHORT, 16);

            val width = right - left + 1;
            val height = bottom - top + 1;

            Logger.trace("width: %d, height: %d", width, height);
            return new Dimension(width, height);
        } catch (Throwable e) {
            return new Dimension(0, 0);
        }
    }

}
