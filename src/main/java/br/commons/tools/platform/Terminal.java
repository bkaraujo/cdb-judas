package br.commons.tools.platform;

import br.commons.Logger;
import br.commons.tools.Platform;
import br.commons.tools.platform.provider.linux.LNXTerminal;
import br.commons.tools.platform.provider.windows.WINTerminal;
import org.jspecify.annotations.NullMarked;

import java.awt.*;

/**
 * Platform-agnostic terminal interface.
 * Provides terminal dimension, raw mode control, and output operations.
 */
@NullMarked
public interface Terminal {

    /**
     * Creates a platform-specific Terminal implementation.
     */
    static Terminal create() {
        if (Platform.CURRENT == OS.LINUX) return new LNXTerminal();
        if (Platform.CURRENT == OS.WINDOWS) return new WINTerminal();

        Logger.fatal("Unsupported Platform");
        throw new RuntimeException("unreachable");
    }

    // ========================================================================
    // Query
    // ========================================================================

    /**
     * Checks if the terminal is a TTY (connected to a terminal).
     */
    default boolean hasTTY() {
        return System.console() != null;
    }

    /**
     * Gets the terminal dimensions (width x height in characters).
     * @return Dimension with width and height, or EMPTY_DIMENSION if unavailable
     */
    Dimension dimension();

    // ========================================================================
    // Output
    // ========================================================================

    /**
     * Renders changed cells from the buffer to the terminal.
     * @param buffer MemorySegment containing INT code points
     * @param dimension terminal dimensions
     * @param indices array of changed cell indices to render
     * @param count number of indices to process
     */
    void render(java.lang.foreign.MemorySegment buffer, Dimension dimension, int[] indices, int count);

}
