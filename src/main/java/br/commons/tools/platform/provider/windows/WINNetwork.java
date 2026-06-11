package br.commons.tools.platform.provider.windows;

import br.commons.Logger;
import br.commons.tools.platform.Network;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Windows implementation of Network using Java FFM (iphlpapi).
 * Usa {@link Iphlpapi#GetExtendedTcpTable} com {@code TCP_TABLE_OWNER_PID_LISTENER}, considerando
 * apenas sockets em LISTEN — estados transitórios como TIME_WAIT não constam nessa tabela.
 */
@NullMarked
public class WINNetwork implements Network {

    private static final int AF_INET  = 2;
    private static final int AF_INET6 = 23;
    private static final int TCP_TABLE_OWNER_PID_LISTENER = 3;
    private static final int NO_ERROR = 0;

    // MIB_TCPROW_OWNER_PID (IPv4): 24 bytes, dwLocalPort @ offset 8.
    private static final int ROW_V4_SIZE = 24;
    private static final int ROW_V4_PORT = 8;
    // MIB_TCP6ROW_OWNER_PID (IPv6): 56 bytes, dwLocalPort @ offset 20.
    private static final int ROW_V6_SIZE = 56;
    private static final int ROW_V6_PORT = 20;

    @Override
    public boolean isPortInUse(int port) {
        return hasListener(AF_INET, ROW_V4_SIZE, ROW_V4_PORT, port)
            || hasListener(AF_INET6, ROW_V6_SIZE, ROW_V6_PORT, port);
    }

    private boolean hasListener(int af, int rowSize, int portOffset, int port) {
        try (val localArena = Arena.ofConfined()) {
            val sizeRef = localArena.allocate(ValueLayout.JAVA_INT);

            // 1ª chamada (buffer nulo): descobre o tamanho necessário em sizeRef.
            Iphlpapi.GetExtendedTcpTable(MemorySegment.NULL, sizeRef, WinAPI.FALSE, af, TCP_TABLE_OWNER_PID_LISTENER, 0);

            val size = sizeRef.get(ValueLayout.JAVA_INT, 0);
            if (size <= 0) return false;

            val table = localArena.allocate(size);
            val result = Iphlpapi.GetExtendedTcpTable(table, sizeRef, WinAPI.FALSE, af, TCP_TABLE_OWNER_PID_LISTENER, 0);
            if (result != NO_ERROR) {
                Logger.warn("GetExtendedTcpTable (af=%d) falhou: %d", af, result);
                return false;
            }

            // table: { DWORD dwNumEntries; ROW table[]; } — linhas começam no offset 4.
            val count = table.get(ValueLayout.JAVA_INT, 0);
            for (int i = 0; i < count; i++) {
                val rowOffset = 4L + (long) i * rowSize;
                val rawPort = table.get(ValueLayout.JAVA_INT, rowOffset + portOffset);
                if (ntohs(rawPort) == port) return true;
            }
            return false;
        } catch (Throwable throwable) {
            Logger.warn("Falha ao consultar tabela TCP (af=%d): %s", af, throwable.getMessage());
            return false;
        }
    }

    // dwLocalPort guarda a porta em network byte order nos 16 bits baixos do DWORD.
    private int ntohs(int value) {
        return ((value & 0xFF) << 8) | ((value >> 8) & 0xFF);
    }
}
