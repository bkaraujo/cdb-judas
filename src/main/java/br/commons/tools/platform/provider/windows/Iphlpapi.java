package br.commons.tools.platform.provider.windows;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static br.commons.tools.platform.provider.windows.WinAPI.*;

/**
 * Funções da API Windows iphlpapi.dll (IP Helper).
 *
 * Categorias:
 * - Tabelas TCP/UDP: GetExtendedTcpTable
 */
@NullMarked
public abstract class Iphlpapi {
    private Iphlpapi(){}

    private static final SymbolLookup iphlpapi = SymbolLookup.libraryLookup("iphlpapi", arena);

    // =====================================================================
    // GetExtendedTcpTable
    // =====================================================================
    private static @Nullable MethodHandle getExtendedTcpTable;
    public static int GetExtendedTcpTable(MemorySegment pTcpTable, MemorySegment pdwSize, int bOrder, int ulAf, int tableClass, int reserved) {
        if (getExtendedTcpTable == null) {
            getExtendedTcpTable = linker.downcallHandle(
                    iphlpapi.find("GetExtendedTcpTable").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,   // pTcpTable
                            ValueLayout.ADDRESS,   // pdwSize
                            ValueLayout.JAVA_INT,  // bOrder
                            ValueLayout.JAVA_INT,  // ulAf
                            ValueLayout.JAVA_INT,  // TableClass
                            ValueLayout.JAVA_INT   // Reserved
                    )
            );
        }
        return invoke(getExtendedTcpTable, pTcpTable, pdwSize, bOrder, ulAf, tableClass, reserved);
    }
}
