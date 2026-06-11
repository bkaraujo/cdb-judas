package br.commons.platform.provider.linux;

import br.commons.Logger;
import br.commons.platform.Network;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Linux implementation of Network.
 * Lê {@code /proc/net/tcp} e {@code /proc/net/tcp6} e considera apenas sockets em
 * LISTEN ({@code st == 0A}), ignorando estados transitórios como TIME_WAIT.
 */
@NullMarked
public class LNXNetwork implements Network {

    /** Código do estado TCP_LISTEN na coluna {@code st} de /proc/net/tcp. */
    private static final String TCP_LISTEN = "0A";
    private static final Path PROC_TCP  = Path.of("/proc/net/tcp");
    private static final Path PROC_TCP6 = Path.of("/proc/net/tcp6");

    @Override
    public boolean isPortInUse(int port) {
        return hasListener(PROC_TCP, port) || hasListener(PROC_TCP6, port);
    }

    private boolean hasListener(Path procFile, int port) {
        if (!Files.exists(procFile)) return false;

        try {
            for (val line : Files.readAllLines(procFile)) {
                if (matchesListener(line, port)) return true;
            }
        } catch (IOException exception) {
            Logger.warn("Failed to read %s: %s", procFile, exception.getMessage());
        }
        return false;
    }

    // Linha: "  sl  local_address rem_address st ..."
    // Campo 1 = local_address ("0100007F:1538"); campo 3 = st ("0A" = LISTEN).
    // O cabeçalho não casa: seu campo 3 é a string "st", não "0A".
    private boolean matchesListener(String line, int port) {
        val fields = line.trim().split("\\s+");
        if (fields.length < 4 || !TCP_LISTEN.equals(fields[3])) return false;

        val colon = fields[1].lastIndexOf(':');
        if (colon < 0) return false;

        try {
            return Integer.parseInt(fields[1].substring(colon + 1), 16) == port;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
