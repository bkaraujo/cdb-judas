package br.commons.platform.provider.windows;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static br.commons.platform.provider.windows.WinAPI.*;

/**
 * Funções da API Windows Shell32.dll.
 *
 * Categorias:
 * - Execução: ShellExecuteW, ShellExecuteExW
 * - Arquivos/Pastas: SHFileOperationW, SHGetFileInfoW, SHGetFolderPathW, SHGetKnownFolderPath
 * - Drag & Drop: DragQueryFileW, DragFinish, DragAcceptFiles
 * - Ícones: ExtractIconW, ExtractIconExW, Shell_NotifyIconW
 * - Diálogos: SHBrowseForFolderW, SHGetPathFromIDListW
 * - Sistema: SHChangeNotify, CommandLineToArgvW
 */
@NullMarked
public abstract class Shell32 {
    private Shell32(){}

    private static final SymbolLookup shell32 = SymbolLookup.libraryLookup("shell32", arena);

    // =====================================================================
    // ShellExecuteW
    // =====================================================================
    private static @Nullable MethodHandle shellExecuteW;
    public static MemorySegment ShellExecuteW(MemorySegment hwnd, MemorySegment lpOperation, MemorySegment lpFile, MemorySegment lpParameters, MemorySegment lpDirectory, int nShowCmd) {
        if (shellExecuteW == null) {
            shellExecuteW = linker.downcallHandle(
                    shell32.find("ShellExecuteW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }
        return invoke(shellExecuteW, hwnd, lpOperation, lpFile, lpParameters, lpDirectory, nShowCmd);
    }

    // =====================================================================
    // ShellExecuteExW
    // =====================================================================
    private static @Nullable MethodHandle shellExecuteExW;
    public static int ShellExecuteExW(MemorySegment pExecInfo) {
        if (shellExecuteExW == null) {
            shellExecuteExW = linker.downcallHandle(
                    shell32.find("ShellExecuteExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shellExecuteExW, pExecInfo);
    }

    // =====================================================================
    // SHFileOperationW
    // =====================================================================
    private static @Nullable MethodHandle shFileOperationW;
    public static int SHFileOperationW(MemorySegment lpFileOp) {
        if (shFileOperationW == null) {
            shFileOperationW = linker.downcallHandle(
                    shell32.find("SHFileOperationW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shFileOperationW, lpFileOp);
    }

    // =====================================================================
    // SHGetFileInfoW
    // =====================================================================
    private static @Nullable MethodHandle shGetFileInfoW;
    public static long SHGetFileInfoW(MemorySegment pszPath, int dwFileAttributes, MemorySegment psfi, int cbFileInfo, int uFlags) {
        if (shGetFileInfoW == null) {
            shGetFileInfoW = linker.downcallHandle(
                    shell32.find("SHGetFileInfoW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }
        return invoke(shGetFileInfoW, pszPath, dwFileAttributes, psfi, cbFileInfo, uFlags);
    }

    // =====================================================================
    // SHGetFolderPathW
    // =====================================================================
    private static @Nullable MethodHandle shGetFolderPathW;
    public static int SHGetFolderPathW(MemorySegment hwnd, int csidl, MemorySegment hToken, int dwFlags, MemorySegment pszPath) {
        if (shGetFolderPathW == null) {
            shGetFolderPathW = linker.downcallHandle(
                    shell32.find("SHGetFolderPathW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shGetFolderPathW, hwnd, csidl, hToken, dwFlags, pszPath);
    }

    // =====================================================================
    // SHGetKnownFolderPath
    // =====================================================================
    private static @Nullable MethodHandle shGetKnownFolderPath;
    public static int SHGetKnownFolderPath(MemorySegment rfid, int dwFlags, MemorySegment hToken, MemorySegment ppszPath) {
        if (shGetKnownFolderPath == null) {
            shGetKnownFolderPath = linker.downcallHandle(
                    shell32.find("SHGetKnownFolderPath").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shGetKnownFolderPath, rfid, dwFlags, hToken, ppszPath);
    }

    // =====================================================================
    // DragQueryFileW
    // =====================================================================
    private static @Nullable MethodHandle dragQueryFileW;
    public static int DragQueryFileW(MemorySegment hDrop, int iFile, MemorySegment lpszFile, int cch) {
        if (dragQueryFileW == null) {
            dragQueryFileW = linker.downcallHandle(
                    shell32.find("DragQueryFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }
        return invoke(dragQueryFileW, hDrop, iFile, lpszFile, cch);
    }

    // =====================================================================
    // DragFinish
    // =====================================================================
    private static @Nullable MethodHandle dragFinish;
    public static void DragFinish(MemorySegment hDrop) {
        if (dragFinish == null) {
            dragFinish = linker.downcallHandle(
                    shell32.find("DragFinish").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }
        invoke(dragFinish, hDrop);
    }

    // =====================================================================
    // DragAcceptFiles
    // =====================================================================
    private static @Nullable MethodHandle dragAcceptFiles;
    public static void DragAcceptFiles(MemorySegment hWnd, int fAccept) {
        if (dragAcceptFiles == null) {
            dragAcceptFiles = linker.downcallHandle(
                    shell32.find("DragAcceptFiles").orElseThrow(),
                    FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }
        invoke(dragAcceptFiles, hWnd, fAccept);
    }

    // =====================================================================
    // ExtractIconW
    // =====================================================================
    private static @Nullable MethodHandle extractIconW;
    public static MemorySegment ExtractIconW(MemorySegment hInst, MemorySegment lpszExeFileName, int nIconIndex) {
        if (extractIconW == null) {
            extractIconW = linker.downcallHandle(
                    shell32.find("ExtractIconW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }
        return invoke(extractIconW, hInst, lpszExeFileName, nIconIndex);
    }

    // =====================================================================
    // ExtractIconExW
    // =====================================================================
    private static @Nullable MethodHandle extractIconExW;
    public static int ExtractIconExW(MemorySegment lpszFile, int nIconIndex, MemorySegment phiconLarge, MemorySegment phiconSmall, int nIcons) {
        if (extractIconExW == null) {
            extractIconExW = linker.downcallHandle(
                    shell32.find("ExtractIconExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }
        return invoke(extractIconExW, lpszFile, nIconIndex, phiconLarge, phiconSmall, nIcons);
    }

    // =====================================================================
    // Shell_NotifyIconW
    // =====================================================================
    private static @Nullable MethodHandle shell_NotifyIconW;
    public static int Shell_NotifyIconW(int dwMessage, MemorySegment lpData) {
        if (shell_NotifyIconW == null) {
            shell_NotifyIconW = linker.downcallHandle(
                    shell32.find("Shell_NotifyIconW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shell_NotifyIconW, dwMessage, lpData);
    }

    // =====================================================================
    // SHBrowseForFolderW
    // =====================================================================
    private static @Nullable MethodHandle shBrowseForFolderW;
    public static MemorySegment SHBrowseForFolderW(MemorySegment lpbi) {
        if (shBrowseForFolderW == null) {
            shBrowseForFolderW = linker.downcallHandle(
                    shell32.find("SHBrowseForFolderW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shBrowseForFolderW, lpbi);
    }

    // =====================================================================
    // SHGetPathFromIDListW
    // =====================================================================
    private static @Nullable MethodHandle shGetPathFromIDListW;
    public static int SHGetPathFromIDListW(MemorySegment pidl, MemorySegment pszPath) {
        if (shGetPathFromIDListW == null) {
            shGetPathFromIDListW = linker.downcallHandle(
                    shell32.find("SHGetPathFromIDListW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(shGetPathFromIDListW, pidl, pszPath);
    }

    // =====================================================================
    // CommandLineToArgvW
    // =====================================================================
    private static @Nullable MethodHandle commandLineToArgvW;
    public static MemorySegment CommandLineToArgvW(MemorySegment lpCmdLine, MemorySegment pNumArgs) {
        if (commandLineToArgvW == null) {
            commandLineToArgvW = linker.downcallHandle(
                    shell32.find("CommandLineToArgvW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }
        return invoke(commandLineToArgvW, lpCmdLine, pNumArgs);
    }

    // =====================================================================
    // SHChangeNotify
    // =====================================================================
    private static @Nullable MethodHandle shChangeNotify;
    public static void SHChangeNotify(int wEventId, int uFlags, MemorySegment dwItem1, MemorySegment dwItem2) {
        if (shChangeNotify == null) {
            shChangeNotify = linker.downcallHandle(
                    shell32.find("SHChangeNotify").orElseThrow(),
                    FunctionDescriptor.ofVoid(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }
        invoke(shChangeNotify, wEventId, uFlags, dwItem1, dwItem2);
    }
}