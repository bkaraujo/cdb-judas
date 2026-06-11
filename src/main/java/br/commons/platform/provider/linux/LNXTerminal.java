package br.commons.platform.provider.linux;

import br.commons.platform.provider.agnostic.AbstractTerminal;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Linux implementation of Terminal using Java FFM.
 * Uses ioctl for dimension and termios for raw mode.
 */
@NullMarked
public class LNXTerminal extends AbstractTerminal {

    private final MethodHandle ioctl;

    public LNXTerminal() {
        val linker = Linker.nativeLinker();
        val lookup = linker.defaultLookup();

        // ioctl(int fd, unsigned long request, ...)
        ioctl = linker.downcallHandle(
                lookup.find("ioctl").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS
                )
        );
    }

    // ========================================================================
    // Dimension
    // ========================================================================

    @Override
    public Dimension dimension() {
        try (val localArena = Arena.ofConfined()) {
            // struct winsize { unsigned short ws_row, ws_col, ws_xpixel, ws_ypixel; }
            val winsize = localArena.allocate(8);

            val result = (int) ioctl.invoke(1, 0x5413, winsize);
            if (result != 0) { return new Dimension(0, 0); }

            val rows = winsize.get(ValueLayout.JAVA_SHORT, 0);
            val cols = winsize.get(ValueLayout.JAVA_SHORT, 2);
            return new Dimension(cols, rows);
        } catch (Throwable e) {
            return new Dimension(0, 0);
        }
    }



}
