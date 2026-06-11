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
 * Kernel32.dll Windows API functions for user interface operations.
 *
 *   - Device handles: STD_INPUT_HANDLE, STD_OUTPUT_HANDLE, STD_ERROR_HANDLE
 *   - Console mode flags: input e output (ENABLE_VIRTUAL_TERMINAL_PROCESSING, etc.)
 *   - Text attributes: foreground/background colors
 *   - File access: GENERIC_READ, GENERIC_WRITE, etc.
 *   - File share mode: FILE_SHARE_READ, FILE_SHARE_WRITE, FILE_SHARE_DELETE
 *   - File creation: CREATE_NEW, OPEN_EXISTING, etc.
 *   - File attributes: FILE_ATTRIBUTE_READONLY, HIDDEN, DIRECTORY, etc.
 *   - File flags: FILE_FLAG_OVERLAPPED, NO_BUFFERING, etc.
 *   - Memory allocation: MEM_COMMIT, MEM_RESERVE, MEM_RELEASE
 *   - Memory protection: PAGE_READONLY, PAGE_READWRITE, PAGE_EXECUTE, etc.
 *   - Process/Thread access rights
 *   - Heap flags, Global/Local memory flags
 *   - Wait return values: WAIT_OBJECT_0, WAIT_TIMEOUT, INFINITE
 *   - Pipe constants, Drive types, Error codes
 *   - Thread priority, Priority classes
 *   - Console control events, Startup info flags, Show window commands
 *   - Processor architecture
 *
 *   Funções do Kernel32 (~120)
 *
 *   - Console: GetStdHandle, Get/SetConsoleMode, GetConsoleScreenBufferInfo, SetConsoleCursorPosition, SetConsoleTextAttribute, Read/WriteConsoleW, FillConsoleOutputCharacterW, AllocConsole, FreeConsole, AttachConsole, SetConsoleCtrlHandler, etc.
 *   - File I/O: CreateFileW, ReadFile, WriteFile, CloseHandle, SetFilePointer, GetFileSize, DeleteFileW, CopyFileW, MoveFileW, GetFileAttributesW, CreateDirectoryW, FindFirstFileW, FindNextFileW, etc.
 *   - Process/Thread: CreateProcessW, OpenProcess, TerminateProcess, GetCurrentProcess, CreateThread, SuspendThread, ResumeThread, Sleep, WaitForSingleObject, WaitForMultipleObjects, etc.
 *   - Memory: VirtualAlloc, VirtualFree, VirtualProtect, HeapAlloc, HeapFree, GlobalAlloc, LocalAlloc, etc.
 *   - Synchronization: CreateEventW, SetEvent, CreateMutexW, ReleaseMutex, CreateSemaphoreW, InitializeCriticalSection, etc.
 *   - Environment: GetEnvironmentVariableW, SetEnvironmentVariableW, GetComputerNameW
 *   - System: GetSystemInfo, GetVersionExW, GetTickCount64, QueryPerformanceCounter, GetSystemTime, GetLocalTime
 *   - Module: LoadLibraryW, FreeLibrary, GetProcAddress, GetModuleHandleW
 *   - File mapping: CreateFileMappingW, MapViewOfFile, UnmapViewOfFile, LockFile
 *   - Pipe: CreatePipe, CreateNamedPipeW, ConnectNamedPipe, PeekNamedPipe
 *   - Misc: FormatMessageW, MultiByteToWideChar, WideCharToMultiByte, DuplicateHandle, DeviceIoControl, etc.
 */
@NullMarked
public abstract class Kernel32 {
    private Kernel32(){}

    private static final SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", arena);

    public static final MemorySegment hStdin  = GetStdHandle(WinCon.STD_INPUT_HANDLE);
    public static final MemorySegment hStdout = GetStdHandle(WinCon.STD_OUTPUT_HANDLE);
    public static final MemorySegment hStderr = GetStdHandle(WinCon.STD_ERROR_HANDLE);

    // =====================================================================
    // GetStdHandle
    // =====================================================================
    private static @Nullable MethodHandle getStdHandle;
    public static MemorySegment GetStdHandle(int handle) {
        if (getStdHandle == null) {
            getStdHandle = linker.downcallHandle(
                    kernel32.find("GetStdHandle").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );
        }

        return invoke(getStdHandle, handle);
    }

    // =====================================================================
    // GetLastError
    // =====================================================================
    private static @Nullable MethodHandle getLastError;
    public static int GetLastError() {
        if (getLastError == null) {
            getLastError = linker.downcallHandle(
                    kernel32.find("GetLastError").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getLastError);
    }

    // =====================================================================
    // GetConsoleMode
    // =====================================================================
    private static @Nullable MethodHandle getConsoleMode;
    public static int GetConsoleMode(MemorySegment hConsoleHandle, MemorySegment lpMode) {
        if (getConsoleMode == null) {
            getConsoleMode = linker.downcallHandle(
                    kernel32.find("GetConsoleMode").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getConsoleMode, hConsoleHandle, lpMode);
    }

    // =====================================================================
    // SetConsoleMode
    // =====================================================================
    private static @Nullable MethodHandle setConsoleMode;
    public static int SetConsoleMode(MemorySegment hConsoleHandle, int dwMode) {
        if (setConsoleMode == null) {
            setConsoleMode = linker.downcallHandle(
                    kernel32.find("SetConsoleMode").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setConsoleMode, hConsoleHandle, dwMode);
    }

    // =====================================================================
    // GetConsoleScreenBufferInfo
    // =====================================================================
    private static @Nullable MethodHandle getConsoleScreenBufferInfo;
    public static int GetConsoleScreenBufferInfo(MemorySegment hConsoleOutput, MemorySegment lpConsoleScreenBufferInfo) {
        if (getConsoleScreenBufferInfo == null) {
            getConsoleScreenBufferInfo = linker.downcallHandle(
                    kernel32.find("GetConsoleScreenBufferInfo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getConsoleScreenBufferInfo, hConsoleOutput, lpConsoleScreenBufferInfo);
    }

    // =====================================================================
    // SetConsoleCursorPosition
    // =====================================================================
    private static @Nullable MethodHandle setConsoleCursorPosition;
    public static int SetConsoleCursorPosition(MemorySegment hConsoleOutput, int dwCursorPosition) {
        if (setConsoleCursorPosition == null) {
            setConsoleCursorPosition = linker.downcallHandle(
                    kernel32.find("SetConsoleCursorPosition").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT  // COORD is passed by value as DWORD
                    )
            );
        }

        return invoke(setConsoleCursorPosition, hConsoleOutput, dwCursorPosition);
    }

    // =====================================================================
    // SetConsoleTextAttribute
    // =====================================================================
    private static @Nullable MethodHandle setConsoleTextAttribute;
    public static int SetConsoleTextAttribute(MemorySegment hConsoleOutput, short wAttributes) {
        if (setConsoleTextAttribute == null) {
            setConsoleTextAttribute = linker.downcallHandle(
                    kernel32.find("SetConsoleTextAttribute").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_SHORT
                    )
            );
        }

        return invoke(setConsoleTextAttribute, hConsoleOutput, wAttributes);
    }

    // =====================================================================
    // GetConsoleCursorInfo
    // =====================================================================
    private static @Nullable MethodHandle getConsoleCursorInfo;
    public static int GetConsoleCursorInfo(MemorySegment hConsoleOutput, MemorySegment lpConsoleCursorInfo) {
        if (getConsoleCursorInfo == null) {
            getConsoleCursorInfo = linker.downcallHandle(
                    kernel32.find("GetConsoleCursorInfo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getConsoleCursorInfo, hConsoleOutput, lpConsoleCursorInfo);
    }

    // =====================================================================
    // SetConsoleCursorInfo
    // =====================================================================
    private static @Nullable MethodHandle setConsoleCursorInfo;
    public static int SetConsoleCursorInfo(MemorySegment hConsoleOutput, MemorySegment lpConsoleCursorInfo) {
        if (setConsoleCursorInfo == null) {
            setConsoleCursorInfo = linker.downcallHandle(
                    kernel32.find("SetConsoleCursorInfo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setConsoleCursorInfo, hConsoleOutput, lpConsoleCursorInfo);
    }

    // =====================================================================
    // WriteConsoleW
    // =====================================================================
    private static @Nullable MethodHandle writeConsoleW;
    public static int WriteConsoleW(MemorySegment hConsoleOutput, MemorySegment lpBuffer, int nNumberOfCharsToWrite, MemorySegment lpNumberOfCharsWritten, MemorySegment lpReserved) {
        if (writeConsoleW == null) {
            writeConsoleW = linker.downcallHandle(
                    kernel32.find("WriteConsoleW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(writeConsoleW, hConsoleOutput, lpBuffer, nNumberOfCharsToWrite, lpNumberOfCharsWritten, lpReserved);
    }

    // =====================================================================
    // ReadConsoleW
    // =====================================================================
    private static @Nullable MethodHandle readConsoleW;
    public static int ReadConsoleW(MemorySegment hConsoleInput, MemorySegment lpBuffer, int nNumberOfCharsToRead, MemorySegment lpNumberOfCharsRead, MemorySegment pInputControl) {
        if (readConsoleW == null) {
            readConsoleW = linker.downcallHandle(
                    kernel32.find("ReadConsoleW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(readConsoleW, hConsoleInput, lpBuffer, nNumberOfCharsToRead, lpNumberOfCharsRead, pInputControl);
    }

    // =====================================================================
    // ReadConsoleInputW
    // =====================================================================
    private static @Nullable MethodHandle readConsoleInputW;
    public static int ReadConsoleInputW(MemorySegment hConsoleInput, MemorySegment lpBuffer, int nLength, MemorySegment lpNumberOfEventsRead) {
        if (readConsoleInputW == null) {
            readConsoleInputW = linker.downcallHandle(
                    kernel32.find("ReadConsoleInputW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(readConsoleInputW, hConsoleInput, lpBuffer, nLength, lpNumberOfEventsRead);
    }

    // =====================================================================
    // FillConsoleOutputCharacterW
    // =====================================================================
    private static @Nullable MethodHandle fillConsoleOutputCharacterW;
    public static int FillConsoleOutputCharacterW(MemorySegment hConsoleOutput, char cCharacter, int nLength, int dwWriteCoord, MemorySegment lpNumberOfCharsWritten) {
        if (fillConsoleOutputCharacterW == null) {
            fillConsoleOutputCharacterW = linker.downcallHandle(
                    kernel32.find("FillConsoleOutputCharacterW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_CHAR,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,  // COORD passed by value
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(fillConsoleOutputCharacterW, hConsoleOutput, cCharacter, nLength, dwWriteCoord, lpNumberOfCharsWritten);
    }

    // =====================================================================
    // FillConsoleOutputAttribute
    // =====================================================================
    private static @Nullable MethodHandle fillConsoleOutputAttribute;
    public static int FillConsoleOutputAttribute(MemorySegment hConsoleOutput, short wAttribute, int nLength, int dwWriteCoord, MemorySegment lpNumberOfAttrsWritten) {
        if (fillConsoleOutputAttribute == null) {
            fillConsoleOutputAttribute = linker.downcallHandle(
                    kernel32.find("FillConsoleOutputAttribute").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_SHORT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,  // COORD passed by value
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(fillConsoleOutputAttribute, hConsoleOutput, wAttribute, nLength, dwWriteCoord, lpNumberOfAttrsWritten);
    }

    // =====================================================================
    // SetConsoleTitleW
    // =====================================================================
    private static @Nullable MethodHandle setConsoleTitleW;
    public static int SetConsoleTitleW(MemorySegment lpConsoleTitle) {
        if (setConsoleTitleW == null) {
            setConsoleTitleW = linker.downcallHandle(
                    kernel32.find("SetConsoleTitleW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setConsoleTitleW, lpConsoleTitle);
    }

    // =====================================================================
    // GetConsoleTitleW
    // =====================================================================
    private static @Nullable MethodHandle getConsoleTitleW;
    public static int GetConsoleTitleW(MemorySegment lpConsoleTitle, int nSize) {
        if (getConsoleTitleW == null) {
            getConsoleTitleW = linker.downcallHandle(
                    kernel32.find("GetConsoleTitleW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getConsoleTitleW, lpConsoleTitle, nSize);
    }

    // =====================================================================
    // SetConsoleCP
    // =====================================================================
    private static @Nullable MethodHandle setConsoleCP;
    public static int SetConsoleCP(int wCodePageID) {
        if (setConsoleCP == null) {
            setConsoleCP = linker.downcallHandle(
                    kernel32.find("SetConsoleCP").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setConsoleCP, wCodePageID);
    }

    // =====================================================================
    // GetConsoleCP
    // =====================================================================
    private static @Nullable MethodHandle getConsoleCP;
    public static int GetConsoleCP() {
        if (getConsoleCP == null) {
            getConsoleCP = linker.downcallHandle(
                    kernel32.find("GetConsoleCP").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getConsoleCP);
    }

    // =====================================================================
    // SetConsoleOutputCP
    // =====================================================================
    private static @Nullable MethodHandle setConsoleOutputCP;
    public static int SetConsoleOutputCP(int wCodePageID) {
        if (setConsoleOutputCP == null) {
            setConsoleOutputCP = linker.downcallHandle(
                    kernel32.find("SetConsoleOutputCP").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setConsoleOutputCP, wCodePageID);
    }

    // =====================================================================
    // GetConsoleOutputCP
    // =====================================================================
    private static @Nullable MethodHandle getConsoleOutputCP;
    public static int GetConsoleOutputCP() {
        if (getConsoleOutputCP == null) {
            getConsoleOutputCP = linker.downcallHandle(
                    kernel32.find("GetConsoleOutputCP").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getConsoleOutputCP);
    }

    // =====================================================================
    // GetNumberOfConsoleInputEvents
    // =====================================================================
    private static @Nullable MethodHandle getNumberOfConsoleInputEvents;
    public static int GetNumberOfConsoleInputEvents(MemorySegment hConsoleInput, MemorySegment lpcNumberOfEvents) {
        if (getNumberOfConsoleInputEvents == null) {
            getNumberOfConsoleInputEvents = linker.downcallHandle(
                    kernel32.find("GetNumberOfConsoleInputEvents").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getNumberOfConsoleInputEvents, hConsoleInput, lpcNumberOfEvents);
    }

    // =====================================================================
    // FlushConsoleInputBuffer
    // =====================================================================
    private static @Nullable MethodHandle flushConsoleInputBuffer;
    public static int FlushConsoleInputBuffer(MemorySegment hConsoleInput) {
        if (flushConsoleInputBuffer == null) {
            flushConsoleInputBuffer = linker.downcallHandle(
                    kernel32.find("FlushConsoleInputBuffer").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(flushConsoleInputBuffer, hConsoleInput);
    }

    // =====================================================================
    // ScrollConsoleScreenBufferW
    // =====================================================================
    private static @Nullable MethodHandle scrollConsoleScreenBufferW;
    public static int ScrollConsoleScreenBufferW(MemorySegment hConsoleOutput, MemorySegment lpScrollRectangle, MemorySegment lpClipRectangle, int dwDestinationOrigin, MemorySegment lpFill) {
        if (scrollConsoleScreenBufferW == null) {
            scrollConsoleScreenBufferW = linker.downcallHandle(
                    kernel32.find("ScrollConsoleScreenBufferW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,  // COORD passed by value
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(scrollConsoleScreenBufferW, hConsoleOutput, lpScrollRectangle, lpClipRectangle, dwDestinationOrigin, lpFill);
    }

    // =====================================================================
    // SetConsoleScreenBufferSize
    // =====================================================================
    private static @Nullable MethodHandle setConsoleScreenBufferSize;
    public static int SetConsoleScreenBufferSize(MemorySegment hConsoleOutput, int dwSize) {
        if (setConsoleScreenBufferSize == null) {
            setConsoleScreenBufferSize = linker.downcallHandle(
                    kernel32.find("SetConsoleScreenBufferSize").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT  // COORD passed by value
                    )
            );
        }

        return invoke(setConsoleScreenBufferSize, hConsoleOutput, dwSize);
    }

    // =====================================================================
    // SetConsoleWindowInfo
    // =====================================================================
    private static @Nullable MethodHandle setConsoleWindowInfo;
    public static int SetConsoleWindowInfo(MemorySegment hConsoleOutput, int bAbsolute, MemorySegment lpConsoleWindow) {
        if (setConsoleWindowInfo == null) {
            setConsoleWindowInfo = linker.downcallHandle(
                    kernel32.find("SetConsoleWindowInfo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setConsoleWindowInfo, hConsoleOutput, bAbsolute, lpConsoleWindow);
    }

    // =====================================================================
    // WaitForSingleObject
    // =====================================================================
    private static @Nullable MethodHandle waitForSingleObject;
    public static int WaitForSingleObject(MemorySegment hHandle, int dwMilliseconds) {
        if (waitForSingleObject == null) {
            waitForSingleObject = linker.downcallHandle(
                    kernel32.find("WaitForSingleObject").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(waitForSingleObject, hHandle, dwMilliseconds);
    }

    // =====================================================================
    // CreateFileW
    // =====================================================================
    private static @Nullable MethodHandle createFileW;
    public static MemorySegment CreateFileW(MemorySegment lpFileName, int dwDesiredAccess, int dwShareMode, MemorySegment lpSecurityAttributes, int dwCreationDisposition, int dwFlagsAndAttributes, MemorySegment hTemplateFile) {
        if (createFileW == null) {
            createFileW = linker.downcallHandle(
                    kernel32.find("CreateFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createFileW, lpFileName, dwDesiredAccess, dwShareMode, lpSecurityAttributes, dwCreationDisposition, dwFlagsAndAttributes, hTemplateFile);
    }

    // =====================================================================
    // ReadFile
    // =====================================================================
    private static @Nullable MethodHandle readFile;
    public static int ReadFile(MemorySegment hFile, MemorySegment lpBuffer, int nNumberOfBytesToRead, MemorySegment lpNumberOfBytesRead, MemorySegment lpOverlapped) {
        if (readFile == null) {
            readFile = linker.downcallHandle(
                    kernel32.find("ReadFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(readFile, hFile, lpBuffer, nNumberOfBytesToRead, lpNumberOfBytesRead, lpOverlapped);
    }

    // =====================================================================
    // WriteFile
    // =====================================================================
    private static @Nullable MethodHandle writeFile;
    public static int WriteFile(MemorySegment hFile, MemorySegment lpBuffer, int nNumberOfBytesToWrite, MemorySegment lpNumberOfBytesWritten, MemorySegment lpOverlapped) {
        if (writeFile == null) {
            writeFile = linker.downcallHandle(
                    kernel32.find("WriteFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(writeFile, hFile, lpBuffer, nNumberOfBytesToWrite, lpNumberOfBytesWritten, lpOverlapped);
    }

    // =====================================================================
    // CloseHandle
    // =====================================================================
    private static @Nullable MethodHandle closeHandle;
    public static int CloseHandle(MemorySegment hObject) {
        if (closeHandle == null) {
            closeHandle = linker.downcallHandle(
                    kernel32.find("CloseHandle").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(closeHandle, hObject);
    }

    // =====================================================================
    // SetFilePointer
    // =====================================================================
    private static @Nullable MethodHandle setFilePointer;
    public static int SetFilePointer(MemorySegment hFile, int lDistanceToMove, MemorySegment lpDistanceToMoveHigh, int dwMoveMethod) {
        if (setFilePointer == null) {
            setFilePointer = linker.downcallHandle(
                    kernel32.find("SetFilePointer").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setFilePointer, hFile, lDistanceToMove, lpDistanceToMoveHigh, dwMoveMethod);
    }

    // =====================================================================
    // SetFilePointerEx
    // =====================================================================
    private static @Nullable MethodHandle setFilePointerEx;
    public static int SetFilePointerEx(MemorySegment hFile, long liDistanceToMove, MemorySegment lpNewFilePointer, int dwMoveMethod) {
        if (setFilePointerEx == null) {
            setFilePointerEx = linker.downcallHandle(
                    kernel32.find("SetFilePointerEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setFilePointerEx, hFile, liDistanceToMove, lpNewFilePointer, dwMoveMethod);
    }

    // =====================================================================
    // GetFileSize
    // =====================================================================
    private static @Nullable MethodHandle getFileSize;
    public static int GetFileSize(MemorySegment hFile, MemorySegment lpFileSizeHigh) {
        if (getFileSize == null) {
            getFileSize = linker.downcallHandle(
                    kernel32.find("GetFileSize").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getFileSize, hFile, lpFileSizeHigh);
    }

    // =====================================================================
    // GetFileSizeEx
    // =====================================================================
    private static @Nullable MethodHandle getFileSizeEx;
    public static int GetFileSizeEx(MemorySegment hFile, MemorySegment lpFileSize) {
        if (getFileSizeEx == null) {
            getFileSizeEx = linker.downcallHandle(
                    kernel32.find("GetFileSizeEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getFileSizeEx, hFile, lpFileSize);
    }

    // =====================================================================
    // GetFileType
    // =====================================================================
    private static @Nullable MethodHandle getFileType;
    public static int GetFileType(MemorySegment hFile) {
        if (getFileType == null) {
            getFileType = linker.downcallHandle(
                    kernel32.find("GetFileType").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getFileType, hFile);
    }

    // =====================================================================
    // FlushFileBuffers
    // =====================================================================
    private static @Nullable MethodHandle flushFileBuffers;
    public static int FlushFileBuffers(MemorySegment hFile) {
        if (flushFileBuffers == null) {
            flushFileBuffers = linker.downcallHandle(
                    kernel32.find("FlushFileBuffers").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(flushFileBuffers, hFile);
    }

    // =====================================================================
    // SetEndOfFile
    // =====================================================================
    private static @Nullable MethodHandle setEndOfFile;
    public static int SetEndOfFile(MemorySegment hFile) {
        if (setEndOfFile == null) {
            setEndOfFile = linker.downcallHandle(
                    kernel32.find("SetEndOfFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setEndOfFile, hFile);
    }

    // =====================================================================
    // DeleteFileW
    // =====================================================================
    private static @Nullable MethodHandle deleteFileW;
    public static int DeleteFileW(MemorySegment lpFileName) {
        if (deleteFileW == null) {
            deleteFileW = linker.downcallHandle(
                    kernel32.find("DeleteFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(deleteFileW, lpFileName);
    }

    // =====================================================================
    // CopyFileW
    // =====================================================================
    private static @Nullable MethodHandle copyFileW;
    public static int CopyFileW(MemorySegment lpExistingFileName, MemorySegment lpNewFileName, int bFailIfExists) {
        if (copyFileW == null) {
            copyFileW = linker.downcallHandle(
                    kernel32.find("CopyFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(copyFileW, lpExistingFileName, lpNewFileName, bFailIfExists);
    }

    // =====================================================================
    // MoveFileW
    // =====================================================================
    private static @Nullable MethodHandle moveFileW;
    public static int MoveFileW(MemorySegment lpExistingFileName, MemorySegment lpNewFileName) {
        if (moveFileW == null) {
            moveFileW = linker.downcallHandle(
                    kernel32.find("MoveFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(moveFileW, lpExistingFileName, lpNewFileName);
    }

    // =====================================================================
    // MoveFileExW
    // =====================================================================
    private static @Nullable MethodHandle moveFileExW;
    public static int MoveFileExW(MemorySegment lpExistingFileName, MemorySegment lpNewFileName, int dwFlags) {
        if (moveFileExW == null) {
            moveFileExW = linker.downcallHandle(
                    kernel32.find("MoveFileExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(moveFileExW, lpExistingFileName, lpNewFileName, dwFlags);
    }

    // =====================================================================
    // GetFileAttributesW
    // =====================================================================
    private static @Nullable MethodHandle getFileAttributesW;
    public static int GetFileAttributesW(MemorySegment lpFileName) {
        if (getFileAttributesW == null) {
            getFileAttributesW = linker.downcallHandle(
                    kernel32.find("GetFileAttributesW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getFileAttributesW, lpFileName);
    }

    // =====================================================================
    // SetFileAttributesW
    // =====================================================================
    private static @Nullable MethodHandle setFileAttributesW;
    public static int SetFileAttributesW(MemorySegment lpFileName, int dwFileAttributes) {
        if (setFileAttributesW == null) {
            setFileAttributesW = linker.downcallHandle(
                    kernel32.find("SetFileAttributesW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setFileAttributesW, lpFileName, dwFileAttributes);
    }

    // =====================================================================
    // CreateDirectoryW
    // =====================================================================
    private static @Nullable MethodHandle createDirectoryW;
    public static int CreateDirectoryW(MemorySegment lpPathName, MemorySegment lpSecurityAttributes) {
        if (createDirectoryW == null) {
            createDirectoryW = linker.downcallHandle(
                    kernel32.find("CreateDirectoryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createDirectoryW, lpPathName, lpSecurityAttributes);
    }

    // =====================================================================
    // RemoveDirectoryW
    // =====================================================================
    private static @Nullable MethodHandle removeDirectoryW;
    public static int RemoveDirectoryW(MemorySegment lpPathName) {
        if (removeDirectoryW == null) {
            removeDirectoryW = linker.downcallHandle(
                    kernel32.find("RemoveDirectoryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(removeDirectoryW, lpPathName);
    }

    // =====================================================================
    // GetCurrentDirectoryW
    // =====================================================================
    private static @Nullable MethodHandle getCurrentDirectoryW;
    public static int GetCurrentDirectoryW(int nBufferLength, MemorySegment lpBuffer) {
        if (getCurrentDirectoryW == null) {
            getCurrentDirectoryW = linker.downcallHandle(
                    kernel32.find("GetCurrentDirectoryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getCurrentDirectoryW, nBufferLength, lpBuffer);
    }

    // =====================================================================
    // SetCurrentDirectoryW
    // =====================================================================
    private static @Nullable MethodHandle setCurrentDirectoryW;
    public static int SetCurrentDirectoryW(MemorySegment lpPathName) {
        if (setCurrentDirectoryW == null) {
            setCurrentDirectoryW = linker.downcallHandle(
                    kernel32.find("SetCurrentDirectoryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setCurrentDirectoryW, lpPathName);
    }

    // =====================================================================
    // FindFirstFileW
    // =====================================================================
    private static @Nullable MethodHandle findFirstFileW;
    public static MemorySegment FindFirstFileW(MemorySegment lpFileName, MemorySegment lpFindFileData) {
        if (findFirstFileW == null) {
            findFirstFileW = linker.downcallHandle(
                    kernel32.find("FindFirstFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(findFirstFileW, lpFileName, lpFindFileData);
    }

    // =====================================================================
    // FindNextFileW
    // =====================================================================
    private static @Nullable MethodHandle findNextFileW;
    public static int FindNextFileW(MemorySegment hFindFile, MemorySegment lpFindFileData) {
        if (findNextFileW == null) {
            findNextFileW = linker.downcallHandle(
                    kernel32.find("FindNextFileW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(findNextFileW, hFindFile, lpFindFileData);
    }

    // =====================================================================
    // FindClose
    // =====================================================================
    private static @Nullable MethodHandle findClose;
    public static int FindClose(MemorySegment hFindFile) {
        if (findClose == null) {
            findClose = linker.downcallHandle(
                    kernel32.find("FindClose").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(findClose, hFindFile);
    }

    // =====================================================================
    // GetTempPathW
    // =====================================================================
    private static @Nullable MethodHandle getTempPathW;
    public static int GetTempPathW(int nBufferLength, MemorySegment lpBuffer) {
        if (getTempPathW == null) {
            getTempPathW = linker.downcallHandle(
                    kernel32.find("GetTempPathW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getTempPathW, nBufferLength, lpBuffer);
    }

    // =====================================================================
    // GetTempFileNameW
    // =====================================================================
    private static @Nullable MethodHandle getTempFileNameW;
    public static int GetTempFileNameW(MemorySegment lpPathName, MemorySegment lpPrefixString, int uUnique, MemorySegment lpTempFileName) {
        if (getTempFileNameW == null) {
            getTempFileNameW = linker.downcallHandle(
                    kernel32.find("GetTempFileNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getTempFileNameW, lpPathName, lpPrefixString, uUnique, lpTempFileName);
    }

    // =====================================================================
    // GetFullPathNameW
    // =====================================================================
    private static @Nullable MethodHandle getFullPathNameW;
    public static int GetFullPathNameW(MemorySegment lpFileName, int nBufferLength, MemorySegment lpBuffer, MemorySegment lpFilePart) {
        if (getFullPathNameW == null) {
            getFullPathNameW = linker.downcallHandle(
                    kernel32.find("GetFullPathNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getFullPathNameW, lpFileName, nBufferLength, lpBuffer, lpFilePart);
    }

    // =====================================================================
    // GetLongPathNameW
    // =====================================================================
    private static @Nullable MethodHandle getLongPathNameW;
    public static int GetLongPathNameW(MemorySegment lpszShortPath, MemorySegment lpszLongPath, int cchBuffer) {
        if (getLongPathNameW == null) {
            getLongPathNameW = linker.downcallHandle(
                    kernel32.find("GetLongPathNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getLongPathNameW, lpszShortPath, lpszLongPath, cchBuffer);
    }

    // =====================================================================
    // GetShortPathNameW
    // =====================================================================
    private static @Nullable MethodHandle getShortPathNameW;
    public static int GetShortPathNameW(MemorySegment lpszLongPath, MemorySegment lpszShortPath, int cchBuffer) {
        if (getShortPathNameW == null) {
            getShortPathNameW = linker.downcallHandle(
                    kernel32.find("GetShortPathNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getShortPathNameW, lpszLongPath, lpszShortPath, cchBuffer);
    }

    // =====================================================================
    // CreateProcessW
    // =====================================================================
    private static @Nullable MethodHandle createProcessW;
    public static int CreateProcessW(MemorySegment lpApplicationName, MemorySegment lpCommandLine, MemorySegment lpProcessAttributes, MemorySegment lpThreadAttributes, int bInheritHandles, int dwCreationFlags, MemorySegment lpEnvironment, MemorySegment lpCurrentDirectory, MemorySegment lpStartupInfo, MemorySegment lpProcessInformation) {
        if (createProcessW == null) {
            createProcessW = linker.downcallHandle(
                    kernel32.find("CreateProcessW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createProcessW, lpApplicationName, lpCommandLine, lpProcessAttributes, lpThreadAttributes, bInheritHandles, dwCreationFlags, lpEnvironment, lpCurrentDirectory, lpStartupInfo, lpProcessInformation);
    }

    // =====================================================================
    // OpenProcess
    // =====================================================================
    private static @Nullable MethodHandle openProcess;
    public static MemorySegment OpenProcess(int dwDesiredAccess, int bInheritHandle, int dwProcessId) {
        if (openProcess == null) {
            openProcess = linker.downcallHandle(
                    kernel32.find("OpenProcess").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(openProcess, dwDesiredAccess, bInheritHandle, dwProcessId);
    }

    // =====================================================================
    // TerminateProcess
    // =====================================================================
    private static @Nullable MethodHandle terminateProcess;
    public static int TerminateProcess(MemorySegment hProcess, int uExitCode) {
        if (terminateProcess == null) {
            terminateProcess = linker.downcallHandle(
                    kernel32.find("TerminateProcess").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(terminateProcess, hProcess, uExitCode);
    }

    // =====================================================================
    // GetExitCodeProcess
    // =====================================================================
    private static @Nullable MethodHandle getExitCodeProcess;
    public static int GetExitCodeProcess(MemorySegment hProcess, MemorySegment lpExitCode) {
        if (getExitCodeProcess == null) {
            getExitCodeProcess = linker.downcallHandle(
                    kernel32.find("GetExitCodeProcess").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getExitCodeProcess, hProcess, lpExitCode);
    }

    // =====================================================================
    // GetCurrentProcess
    // =====================================================================
    private static @Nullable MethodHandle getCurrentProcess;
    public static MemorySegment GetCurrentProcess() {
        if (getCurrentProcess == null) {
            getCurrentProcess = linker.downcallHandle(
                    kernel32.find("GetCurrentProcess").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getCurrentProcess);
    }

    // =====================================================================
    // GetCurrentProcessId
    // =====================================================================
    private static @Nullable MethodHandle getCurrentProcessId;
    public static int GetCurrentProcessId() {
        if (getCurrentProcessId == null) {
            getCurrentProcessId = linker.downcallHandle(
                    kernel32.find("GetCurrentProcessId").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getCurrentProcessId);
    }

    // =====================================================================
    // GetCurrentThread
    // =====================================================================
    private static @Nullable MethodHandle getCurrentThread;
    public static MemorySegment GetCurrentThread() {
        if (getCurrentThread == null) {
            getCurrentThread = linker.downcallHandle(
                    kernel32.find("GetCurrentThread").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getCurrentThread);
    }

    // =====================================================================
    // GetCurrentThreadId
    // =====================================================================
    private static @Nullable MethodHandle getCurrentThreadId;
    public static int GetCurrentThreadId() {
        if (getCurrentThreadId == null) {
            getCurrentThreadId = linker.downcallHandle(
                    kernel32.find("GetCurrentThreadId").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getCurrentThreadId);
    }

    // =====================================================================
    // CreateThread
    // =====================================================================
    private static @Nullable MethodHandle createThread;
    public static MemorySegment CreateThread(MemorySegment lpThreadAttributes, long dwStackSize, MemorySegment lpStartAddress, MemorySegment lpParameter, int dwCreationFlags, MemorySegment lpThreadId) {
        if (createThread == null) {
            createThread = linker.downcallHandle(
                    kernel32.find("CreateThread").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createThread, lpThreadAttributes, dwStackSize, lpStartAddress, lpParameter, dwCreationFlags, lpThreadId);
    }

    // =====================================================================
    // ExitThread
    // =====================================================================
    private static @Nullable MethodHandle exitThread;
    public static void ExitThread(int dwExitCode) {
        if (exitThread == null) {
            exitThread = linker.downcallHandle(
                    kernel32.find("ExitThread").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)
            );
        }

        invoke(exitThread, dwExitCode);
    }

    // =====================================================================
    // SuspendThread
    // =====================================================================
    private static @Nullable MethodHandle suspendThread;
    public static int SuspendThread(MemorySegment hThread) {
        if (suspendThread == null) {
            suspendThread = linker.downcallHandle(
                    kernel32.find("SuspendThread").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(suspendThread, hThread);
    }

    // =====================================================================
    // ResumeThread
    // =====================================================================
    private static @Nullable MethodHandle resumeThread;
    public static int ResumeThread(MemorySegment hThread) {
        if (resumeThread == null) {
            resumeThread = linker.downcallHandle(
                    kernel32.find("ResumeThread").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(resumeThread, hThread);
    }

    // =====================================================================
    // GetExitCodeThread
    // =====================================================================
    private static @Nullable MethodHandle getExitCodeThread;
    public static int GetExitCodeThread(MemorySegment hThread, MemorySegment lpExitCode) {
        if (getExitCodeThread == null) {
            getExitCodeThread = linker.downcallHandle(
                    kernel32.find("GetExitCodeThread").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getExitCodeThread, hThread, lpExitCode);
    }

    // =====================================================================
    // Sleep
    // =====================================================================
    private static @Nullable MethodHandle sleep;
    public static void Sleep(int dwMilliseconds) {
        if (sleep == null) {
            sleep = linker.downcallHandle(
                    kernel32.find("Sleep").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)
            );
        }

        invoke(sleep, dwMilliseconds);
    }

    // =====================================================================
    // SleepEx
    // =====================================================================
    private static @Nullable MethodHandle sleepEx;
    public static int SleepEx(int dwMilliseconds, int bAlertable) {
        if (sleepEx == null) {
            sleepEx = linker.downcallHandle(
                    kernel32.find("SleepEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(sleepEx, dwMilliseconds, bAlertable);
    }

    // =====================================================================
    // WaitForMultipleObjects
    // =====================================================================
    private static @Nullable MethodHandle waitForMultipleObjects;
    public static int WaitForMultipleObjects(int nCount, MemorySegment lpHandles, int bWaitAll, int dwMilliseconds) {
        if (waitForMultipleObjects == null) {
            waitForMultipleObjects = linker.downcallHandle(
                    kernel32.find("WaitForMultipleObjects").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(waitForMultipleObjects, nCount, lpHandles, bWaitAll, dwMilliseconds);
    }

    // =====================================================================
    // VirtualAlloc
    // =====================================================================
    private static @Nullable MethodHandle virtualAlloc;
    public static MemorySegment VirtualAlloc(MemorySegment lpAddress, long dwSize, int flAllocationType, int flProtect) {
        if (virtualAlloc == null) {
            virtualAlloc = linker.downcallHandle(
                    kernel32.find("VirtualAlloc").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(virtualAlloc, lpAddress, dwSize, flAllocationType, flProtect);
    }

    // =====================================================================
    // VirtualFree
    // =====================================================================
    private static @Nullable MethodHandle virtualFree;
    public static int VirtualFree(MemorySegment lpAddress, long dwSize, int dwFreeType) {
        if (virtualFree == null) {
            virtualFree = linker.downcallHandle(
                    kernel32.find("VirtualFree").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(virtualFree, lpAddress, dwSize, dwFreeType);
    }

    // =====================================================================
    // VirtualProtect
    // =====================================================================
    private static @Nullable MethodHandle virtualProtect;
    public static int VirtualProtect(MemorySegment lpAddress, long dwSize, int flNewProtect, MemorySegment lpflOldProtect) {
        if (virtualProtect == null) {
            virtualProtect = linker.downcallHandle(
                    kernel32.find("VirtualProtect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(virtualProtect, lpAddress, dwSize, flNewProtect, lpflOldProtect);
    }

    // =====================================================================
    // VirtualQuery
    // =====================================================================
    private static @Nullable MethodHandle virtualQuery;
    public static long VirtualQuery(MemorySegment lpAddress, MemorySegment lpBuffer, long dwLength) {
        if (virtualQuery == null) {
            virtualQuery = linker.downcallHandle(
                    kernel32.find("VirtualQuery").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(virtualQuery, lpAddress, lpBuffer, dwLength);
    }

    // =====================================================================
    // GetProcessHeap
    // =====================================================================
    private static @Nullable MethodHandle getProcessHeap;
    public static MemorySegment GetProcessHeap() {
        if (getProcessHeap == null) {
            getProcessHeap = linker.downcallHandle(
                    kernel32.find("GetProcessHeap").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getProcessHeap);
    }

    // =====================================================================
    // HeapAlloc
    // =====================================================================
    private static @Nullable MethodHandle heapAlloc;
    public static MemorySegment HeapAlloc(MemorySegment hHeap, int dwFlags, long dwBytes) {
        if (heapAlloc == null) {
            heapAlloc = linker.downcallHandle(
                    kernel32.find("HeapAlloc").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(heapAlloc, hHeap, dwFlags, dwBytes);
    }

    // =====================================================================
    // HeapFree
    // =====================================================================
    private static @Nullable MethodHandle heapFree;
    public static int HeapFree(MemorySegment hHeap, int dwFlags, MemorySegment lpMem) {
        if (heapFree == null) {
            heapFree = linker.downcallHandle(
                    kernel32.find("HeapFree").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(heapFree, hHeap, dwFlags, lpMem);
    }

    // =====================================================================
    // HeapReAlloc
    // =====================================================================
    private static @Nullable MethodHandle heapReAlloc;
    public static MemorySegment HeapReAlloc(MemorySegment hHeap, int dwFlags, MemorySegment lpMem, long dwBytes) {
        if (heapReAlloc == null) {
            heapReAlloc = linker.downcallHandle(
                    kernel32.find("HeapReAlloc").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(heapReAlloc, hHeap, dwFlags, lpMem, dwBytes);
    }

    // =====================================================================
    // HeapSize
    // =====================================================================
    private static @Nullable MethodHandle heapSize;
    public static long HeapSize(MemorySegment hHeap, int dwFlags, MemorySegment lpMem) {
        if (heapSize == null) {
            heapSize = linker.downcallHandle(
                    kernel32.find("HeapSize").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(heapSize, hHeap, dwFlags, lpMem);
    }

    // =====================================================================
    // GlobalAlloc
    // =====================================================================
    private static @Nullable MethodHandle globalAlloc;
    public static MemorySegment GlobalAlloc(int uFlags, long dwBytes) {
        if (globalAlloc == null) {
            globalAlloc = linker.downcallHandle(
                    kernel32.find("GlobalAlloc").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(globalAlloc, uFlags, dwBytes);
    }

    // =====================================================================
    // GlobalFree
    // =====================================================================
    private static @Nullable MethodHandle globalFree;
    public static MemorySegment GlobalFree(MemorySegment hMem) {
        if (globalFree == null) {
            globalFree = linker.downcallHandle(
                    kernel32.find("GlobalFree").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(globalFree, hMem);
    }

    // =====================================================================
    // GlobalLock
    // =====================================================================
    private static @Nullable MethodHandle globalLock;
    public static MemorySegment GlobalLock(MemorySegment hMem) {
        if (globalLock == null) {
            globalLock = linker.downcallHandle(
                    kernel32.find("GlobalLock").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(globalLock, hMem);
    }

    // =====================================================================
    // GlobalUnlock
    // =====================================================================
    private static @Nullable MethodHandle globalUnlock;
    public static int GlobalUnlock(MemorySegment hMem) {
        if (globalUnlock == null) {
            globalUnlock = linker.downcallHandle(
                    kernel32.find("GlobalUnlock").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(globalUnlock, hMem);
    }

    // =====================================================================
    // LocalAlloc
    // =====================================================================
    private static @Nullable MethodHandle localAlloc;
    public static MemorySegment LocalAlloc(int uFlags, long uBytes) {
        if (localAlloc == null) {
            localAlloc = linker.downcallHandle(
                    kernel32.find("LocalAlloc").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(localAlloc, uFlags, uBytes);
    }

    // =====================================================================
    // LocalFree
    // =====================================================================
    private static @Nullable MethodHandle localFree;
    public static MemorySegment LocalFree(MemorySegment hMem) {
        if (localFree == null) {
            localFree = linker.downcallHandle(
                    kernel32.find("LocalFree").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(localFree, hMem);
    }

    // =====================================================================
    // GetEnvironmentVariableW
    // =====================================================================
    private static @Nullable MethodHandle getEnvironmentVariableW;
    public static int GetEnvironmentVariableW(MemorySegment lpName, MemorySegment lpBuffer, int nSize) {
        if (getEnvironmentVariableW == null) {
            getEnvironmentVariableW = linker.downcallHandle(
                    kernel32.find("GetEnvironmentVariableW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getEnvironmentVariableW, lpName, lpBuffer, nSize);
    }

    // =====================================================================
    // SetEnvironmentVariableW
    // =====================================================================
    private static @Nullable MethodHandle setEnvironmentVariableW;
    public static int SetEnvironmentVariableW(MemorySegment lpName, MemorySegment lpValue) {
        if (setEnvironmentVariableW == null) {
            setEnvironmentVariableW = linker.downcallHandle(
                    kernel32.find("SetEnvironmentVariableW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setEnvironmentVariableW, lpName, lpValue);
    }

    // =====================================================================
    // GetEnvironmentStringsW
    // =====================================================================
    private static @Nullable MethodHandle getEnvironmentStringsW;
    public static MemorySegment GetEnvironmentStringsW() {
        if (getEnvironmentStringsW == null) {
            getEnvironmentStringsW = linker.downcallHandle(
                    kernel32.find("GetEnvironmentStringsW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getEnvironmentStringsW);
    }

    // =====================================================================
    // FreeEnvironmentStringsW
    // =====================================================================
    private static @Nullable MethodHandle freeEnvironmentStringsW;
    public static int FreeEnvironmentStringsW(MemorySegment lpszEnvironmentBlock) {
        if (freeEnvironmentStringsW == null) {
            freeEnvironmentStringsW = linker.downcallHandle(
                    kernel32.find("FreeEnvironmentStringsW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(freeEnvironmentStringsW, lpszEnvironmentBlock);
    }

    // =====================================================================
    // GetComputerNameW
    // =====================================================================
    private static @Nullable MethodHandle getComputerNameW;
    public static int GetComputerNameW(MemorySegment lpBuffer, MemorySegment nSize) {
        if (getComputerNameW == null) {
            getComputerNameW = linker.downcallHandle(
                    kernel32.find("GetComputerNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getComputerNameW, lpBuffer, nSize);
    }

    // =====================================================================
    // GetUserNameW (from Advapi32, but commonly used)
    // =====================================================================

    // =====================================================================
    // GetSystemDirectoryW
    // =====================================================================
    private static @Nullable MethodHandle getSystemDirectoryW;
    public static int GetSystemDirectoryW(MemorySegment lpBuffer, int uSize) {
        if (getSystemDirectoryW == null) {
            getSystemDirectoryW = linker.downcallHandle(
                    kernel32.find("GetSystemDirectoryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getSystemDirectoryW, lpBuffer, uSize);
    }

    // =====================================================================
    // GetWindowsDirectoryW
    // =====================================================================
    private static @Nullable MethodHandle getWindowsDirectoryW;
    public static int GetWindowsDirectoryW(MemorySegment lpBuffer, int uSize) {
        if (getWindowsDirectoryW == null) {
            getWindowsDirectoryW = linker.downcallHandle(
                    kernel32.find("GetWindowsDirectoryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getWindowsDirectoryW, lpBuffer, uSize);
    }

    // =====================================================================
    // GetModuleFileNameW
    // =====================================================================
    private static @Nullable MethodHandle getModuleFileNameW;
    public static int GetModuleFileNameW(MemorySegment hModule, MemorySegment lpFilename, int nSize) {
        if (getModuleFileNameW == null) {
            getModuleFileNameW = linker.downcallHandle(
                    kernel32.find("GetModuleFileNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getModuleFileNameW, hModule, lpFilename, nSize);
    }

    // =====================================================================
    // GetModuleHandleW
    // =====================================================================
    private static @Nullable MethodHandle getModuleHandleW;
    public static MemorySegment GetModuleHandleW(MemorySegment lpModuleName) {
        if (getModuleHandleW == null) {
            getModuleHandleW = linker.downcallHandle(
                    kernel32.find("GetModuleHandleW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getModuleHandleW, lpModuleName);
    }

    // =====================================================================
    // LoadLibraryW
    // =====================================================================
    private static @Nullable MethodHandle loadLibraryW;
    public static MemorySegment LoadLibraryW(MemorySegment lpLibFileName) {
        if (loadLibraryW == null) {
            loadLibraryW = linker.downcallHandle(
                    kernel32.find("LoadLibraryW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(loadLibraryW, lpLibFileName);
    }

    // =====================================================================
    // LoadLibraryExW
    // =====================================================================
    private static @Nullable MethodHandle loadLibraryExW;
    public static MemorySegment LoadLibraryExW(MemorySegment lpLibFileName, MemorySegment hFile, int dwFlags) {
        if (loadLibraryExW == null) {
            loadLibraryExW = linker.downcallHandle(
                    kernel32.find("LoadLibraryExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(loadLibraryExW, lpLibFileName, hFile, dwFlags);
    }

    // =====================================================================
    // FreeLibrary
    // =====================================================================
    private static @Nullable MethodHandle freeLibrary;
    public static int FreeLibrary(MemorySegment hLibModule) {
        if (freeLibrary == null) {
            freeLibrary = linker.downcallHandle(
                    kernel32.find("FreeLibrary").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(freeLibrary, hLibModule);
    }

    // =====================================================================
    // GetProcAddress
    // =====================================================================
    private static @Nullable MethodHandle getProcAddress;
    public static MemorySegment GetProcAddress(MemorySegment hModule, MemorySegment lpProcName) {
        if (getProcAddress == null) {
            getProcAddress = linker.downcallHandle(
                    kernel32.find("GetProcAddress").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getProcAddress, hModule, lpProcName);
    }

    // =====================================================================
    // GetSystemInfo
    // =====================================================================
    private static @Nullable MethodHandle getSystemInfo;
    public static void GetSystemInfo(MemorySegment lpSystemInfo) {
        if (getSystemInfo == null) {
            getSystemInfo = linker.downcallHandle(
                    kernel32.find("GetSystemInfo").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(getSystemInfo, lpSystemInfo);
    }

    // =====================================================================
    // GetNativeSystemInfo
    // =====================================================================
    private static @Nullable MethodHandle getNativeSystemInfo;
    public static void GetNativeSystemInfo(MemorySegment lpSystemInfo) {
        if (getNativeSystemInfo == null) {
            getNativeSystemInfo = linker.downcallHandle(
                    kernel32.find("GetNativeSystemInfo").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(getNativeSystemInfo, lpSystemInfo);
    }

    // =====================================================================
    // GetVersionExW
    // =====================================================================
    private static @Nullable MethodHandle getVersionExW;
    public static int GetVersionExW(MemorySegment lpVersionInformation) {
        if (getVersionExW == null) {
            getVersionExW = linker.downcallHandle(
                    kernel32.find("GetVersionExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getVersionExW, lpVersionInformation);
    }

    // =====================================================================
    // GetTickCount
    // =====================================================================
    private static @Nullable MethodHandle getTickCount;
    public static int GetTickCount() {
        if (getTickCount == null) {
            getTickCount = linker.downcallHandle(
                    kernel32.find("GetTickCount").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getTickCount);
    }

    // =====================================================================
    // GetTickCount64
    // =====================================================================
    private static @Nullable MethodHandle getTickCount64;
    public static long GetTickCount64() {
        if (getTickCount64 == null) {
            getTickCount64 = linker.downcallHandle(
                    kernel32.find("GetTickCount64").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG)
            );
        }

        return invoke(getTickCount64);
    }

    // =====================================================================
    // QueryPerformanceCounter
    // =====================================================================
    private static @Nullable MethodHandle queryPerformanceCounter;
    public static int QueryPerformanceCounter(MemorySegment lpPerformanceCount) {
        if (queryPerformanceCounter == null) {
            queryPerformanceCounter = linker.downcallHandle(
                    kernel32.find("QueryPerformanceCounter").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(queryPerformanceCounter, lpPerformanceCount);
    }

    // =====================================================================
    // QueryPerformanceFrequency
    // =====================================================================
    private static @Nullable MethodHandle queryPerformanceFrequency;
    public static int QueryPerformanceFrequency(MemorySegment lpFrequency) {
        if (queryPerformanceFrequency == null) {
            queryPerformanceFrequency = linker.downcallHandle(
                    kernel32.find("QueryPerformanceFrequency").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(queryPerformanceFrequency, lpFrequency);
    }

    // =====================================================================
    // GetSystemTime
    // =====================================================================
    private static @Nullable MethodHandle getSystemTime;
    public static void GetSystemTime(MemorySegment lpSystemTime) {
        if (getSystemTime == null) {
            getSystemTime = linker.downcallHandle(
                    kernel32.find("GetSystemTime").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(getSystemTime, lpSystemTime);
    }

    // =====================================================================
    // GetLocalTime
    // =====================================================================
    private static @Nullable MethodHandle getLocalTime;
    public static void GetLocalTime(MemorySegment lpSystemTime) {
        if (getLocalTime == null) {
            getLocalTime = linker.downcallHandle(
                    kernel32.find("GetLocalTime").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(getLocalTime, lpSystemTime);
    }

    // =====================================================================
    // SetSystemTime
    // =====================================================================
    private static @Nullable MethodHandle setSystemTime;
    public static int SetSystemTime(MemorySegment lpSystemTime) {
        if (setSystemTime == null) {
            setSystemTime = linker.downcallHandle(
                    kernel32.find("SetSystemTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setSystemTime, lpSystemTime);
    }

    // =====================================================================
    // SetLocalTime
    // =====================================================================
    private static @Nullable MethodHandle setLocalTime;
    public static int SetLocalTime(MemorySegment lpSystemTime) {
        if (setLocalTime == null) {
            setLocalTime = linker.downcallHandle(
                    kernel32.find("SetLocalTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setLocalTime, lpSystemTime);
    }

    // =====================================================================
    // GetSystemTimeAsFileTime
    // =====================================================================
    private static @Nullable MethodHandle getSystemTimeAsFileTime;
    public static void GetSystemTimeAsFileTime(MemorySegment lpSystemTimeAsFileTime) {
        if (getSystemTimeAsFileTime == null) {
            getSystemTimeAsFileTime = linker.downcallHandle(
                    kernel32.find("GetSystemTimeAsFileTime").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(getSystemTimeAsFileTime, lpSystemTimeAsFileTime);
    }

    // =====================================================================
    // FileTimeToSystemTime
    // =====================================================================
    private static @Nullable MethodHandle fileTimeToSystemTime;
    public static int FileTimeToSystemTime(MemorySegment lpFileTime, MemorySegment lpSystemTime) {
        if (fileTimeToSystemTime == null) {
            fileTimeToSystemTime = linker.downcallHandle(
                    kernel32.find("FileTimeToSystemTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(fileTimeToSystemTime, lpFileTime, lpSystemTime);
    }

    // =====================================================================
    // SystemTimeToFileTime
    // =====================================================================
    private static @Nullable MethodHandle systemTimeToFileTime;
    public static int SystemTimeToFileTime(MemorySegment lpSystemTime, MemorySegment lpFileTime) {
        if (systemTimeToFileTime == null) {
            systemTimeToFileTime = linker.downcallHandle(
                    kernel32.find("SystemTimeToFileTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(systemTimeToFileTime, lpSystemTime, lpFileTime);
    }

    // =====================================================================
    // CreateEventW
    // =====================================================================
    private static @Nullable MethodHandle createEventW;
    public static MemorySegment CreateEventW(MemorySegment lpEventAttributes, int bManualReset, int bInitialState, MemorySegment lpName) {
        if (createEventW == null) {
            createEventW = linker.downcallHandle(
                    kernel32.find("CreateEventW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createEventW, lpEventAttributes, bManualReset, bInitialState, lpName);
    }

    // =====================================================================
    // OpenEventW
    // =====================================================================
    private static @Nullable MethodHandle openEventW;
    public static MemorySegment OpenEventW(int dwDesiredAccess, int bInheritHandle, MemorySegment lpName) {
        if (openEventW == null) {
            openEventW = linker.downcallHandle(
                    kernel32.find("OpenEventW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(openEventW, dwDesiredAccess, bInheritHandle, lpName);
    }

    // =====================================================================
    // SetEvent
    // =====================================================================
    private static @Nullable MethodHandle setEvent;
    public static int SetEvent(MemorySegment hEvent) {
        if (setEvent == null) {
            setEvent = linker.downcallHandle(
                    kernel32.find("SetEvent").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setEvent, hEvent);
    }

    // =====================================================================
    // ResetEvent
    // =====================================================================
    private static @Nullable MethodHandle resetEvent;
    public static int ResetEvent(MemorySegment hEvent) {
        if (resetEvent == null) {
            resetEvent = linker.downcallHandle(
                    kernel32.find("ResetEvent").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(resetEvent, hEvent);
    }

    // =====================================================================
    // PulseEvent
    // =====================================================================
    private static @Nullable MethodHandle pulseEvent;
    public static int PulseEvent(MemorySegment hEvent) {
        if (pulseEvent == null) {
            pulseEvent = linker.downcallHandle(
                    kernel32.find("PulseEvent").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(pulseEvent, hEvent);
    }

    // =====================================================================
    // CreateMutexW
    // =====================================================================
    private static @Nullable MethodHandle createMutexW;
    public static MemorySegment CreateMutexW(MemorySegment lpMutexAttributes, int bInitialOwner, MemorySegment lpName) {
        if (createMutexW == null) {
            createMutexW = linker.downcallHandle(
                    kernel32.find("CreateMutexW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createMutexW, lpMutexAttributes, bInitialOwner, lpName);
    }

    // =====================================================================
    // OpenMutexW
    // =====================================================================
    private static @Nullable MethodHandle openMutexW;
    public static MemorySegment OpenMutexW(int dwDesiredAccess, int bInheritHandle, MemorySegment lpName) {
        if (openMutexW == null) {
            openMutexW = linker.downcallHandle(
                    kernel32.find("OpenMutexW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(openMutexW, dwDesiredAccess, bInheritHandle, lpName);
    }

    // =====================================================================
    // ReleaseMutex
    // =====================================================================
    private static @Nullable MethodHandle releaseMutex;
    public static int ReleaseMutex(MemorySegment hMutex) {
        if (releaseMutex == null) {
            releaseMutex = linker.downcallHandle(
                    kernel32.find("ReleaseMutex").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(releaseMutex, hMutex);
    }

    // =====================================================================
    // CreateSemaphoreW
    // =====================================================================
    private static @Nullable MethodHandle createSemaphoreW;
    public static MemorySegment CreateSemaphoreW(MemorySegment lpSemaphoreAttributes, int lInitialCount, int lMaximumCount, MemorySegment lpName) {
        if (createSemaphoreW == null) {
            createSemaphoreW = linker.downcallHandle(
                    kernel32.find("CreateSemaphoreW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createSemaphoreW, lpSemaphoreAttributes, lInitialCount, lMaximumCount, lpName);
    }

    // =====================================================================
    // OpenSemaphoreW
    // =====================================================================
    private static @Nullable MethodHandle openSemaphoreW;
    public static MemorySegment OpenSemaphoreW(int dwDesiredAccess, int bInheritHandle, MemorySegment lpName) {
        if (openSemaphoreW == null) {
            openSemaphoreW = linker.downcallHandle(
                    kernel32.find("OpenSemaphoreW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(openSemaphoreW, dwDesiredAccess, bInheritHandle, lpName);
    }

    // =====================================================================
    // ReleaseSemaphore
    // =====================================================================
    private static @Nullable MethodHandle releaseSemaphore;
    public static int ReleaseSemaphore(MemorySegment hSemaphore, int lReleaseCount, MemorySegment lpPreviousCount) {
        if (releaseSemaphore == null) {
            releaseSemaphore = linker.downcallHandle(
                    kernel32.find("ReleaseSemaphore").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(releaseSemaphore, hSemaphore, lReleaseCount, lpPreviousCount);
    }

    // =====================================================================
    // InitializeCriticalSection
    // =====================================================================
    private static @Nullable MethodHandle initializeCriticalSection;
    public static void InitializeCriticalSection(MemorySegment lpCriticalSection) {
        if (initializeCriticalSection == null) {
            initializeCriticalSection = linker.downcallHandle(
                    kernel32.find("InitializeCriticalSection").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(initializeCriticalSection, lpCriticalSection);
    }

    // =====================================================================
    // DeleteCriticalSection
    // =====================================================================
    private static @Nullable MethodHandle deleteCriticalSection;
    public static void DeleteCriticalSection(MemorySegment lpCriticalSection) {
        if (deleteCriticalSection == null) {
            deleteCriticalSection = linker.downcallHandle(
                    kernel32.find("DeleteCriticalSection").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(deleteCriticalSection, lpCriticalSection);
    }

    // =====================================================================
    // EnterCriticalSection
    // =====================================================================
    private static @Nullable MethodHandle enterCriticalSection;
    public static void EnterCriticalSection(MemorySegment lpCriticalSection) {
        if (enterCriticalSection == null) {
            enterCriticalSection = linker.downcallHandle(
                    kernel32.find("EnterCriticalSection").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(enterCriticalSection, lpCriticalSection);
    }

    // =====================================================================
    // LeaveCriticalSection
    // =====================================================================
    private static @Nullable MethodHandle leaveCriticalSection;
    public static void LeaveCriticalSection(MemorySegment lpCriticalSection) {
        if (leaveCriticalSection == null) {
            leaveCriticalSection = linker.downcallHandle(
                    kernel32.find("LeaveCriticalSection").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(leaveCriticalSection, lpCriticalSection);
    }

    // =====================================================================
    // TryEnterCriticalSection
    // =====================================================================
    private static @Nullable MethodHandle tryEnterCriticalSection;
    public static int TryEnterCriticalSection(MemorySegment lpCriticalSection) {
        if (tryEnterCriticalSection == null) {
            tryEnterCriticalSection = linker.downcallHandle(
                    kernel32.find("TryEnterCriticalSection").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(tryEnterCriticalSection, lpCriticalSection);
    }

    // =====================================================================
    // CreatePipe
    // =====================================================================
    private static @Nullable MethodHandle createPipe;
    public static int CreatePipe(MemorySegment hReadPipe, MemorySegment hWritePipe, MemorySegment lpPipeAttributes, int nSize) {
        if (createPipe == null) {
            createPipe = linker.downcallHandle(
                    kernel32.find("CreatePipe").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(createPipe, hReadPipe, hWritePipe, lpPipeAttributes, nSize);
    }

    // =====================================================================
    // CreateNamedPipeW
    // =====================================================================
    private static @Nullable MethodHandle createNamedPipeW;
    public static MemorySegment CreateNamedPipeW(MemorySegment lpName, int dwOpenMode, int dwPipeMode, int nMaxInstances, int nOutBufferSize, int nInBufferSize, int nDefaultTimeOut, MemorySegment lpSecurityAttributes) {
        if (createNamedPipeW == null) {
            createNamedPipeW = linker.downcallHandle(
                    kernel32.find("CreateNamedPipeW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createNamedPipeW, lpName, dwOpenMode, dwPipeMode, nMaxInstances, nOutBufferSize, nInBufferSize, nDefaultTimeOut, lpSecurityAttributes);
    }

    // =====================================================================
    // ConnectNamedPipe
    // =====================================================================
    private static @Nullable MethodHandle connectNamedPipe;
    public static int ConnectNamedPipe(MemorySegment hNamedPipe, MemorySegment lpOverlapped) {
        if (connectNamedPipe == null) {
            connectNamedPipe = linker.downcallHandle(
                    kernel32.find("ConnectNamedPipe").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(connectNamedPipe, hNamedPipe, lpOverlapped);
    }

    // =====================================================================
    // DisconnectNamedPipe
    // =====================================================================
    private static @Nullable MethodHandle disconnectNamedPipe;
    public static int DisconnectNamedPipe(MemorySegment hNamedPipe) {
        if (disconnectNamedPipe == null) {
            disconnectNamedPipe = linker.downcallHandle(
                    kernel32.find("DisconnectNamedPipe").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(disconnectNamedPipe, hNamedPipe);
    }

    // =====================================================================
    // PeekNamedPipe
    // =====================================================================
    private static @Nullable MethodHandle peekNamedPipe;
    public static int PeekNamedPipe(MemorySegment hNamedPipe, MemorySegment lpBuffer, int nBufferSize, MemorySegment lpBytesRead, MemorySegment lpTotalBytesAvail, MemorySegment lpBytesLeftThisMessage) {
        if (peekNamedPipe == null) {
            peekNamedPipe = linker.downcallHandle(
                    kernel32.find("PeekNamedPipe").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(peekNamedPipe, hNamedPipe, lpBuffer, nBufferSize, lpBytesRead, lpTotalBytesAvail, lpBytesLeftThisMessage);
    }

    // =====================================================================
    // DuplicateHandle
    // =====================================================================
    private static @Nullable MethodHandle duplicateHandle;
    public static int DuplicateHandle(MemorySegment hSourceProcessHandle, MemorySegment hSourceHandle, MemorySegment hTargetProcessHandle, MemorySegment lpTargetHandle, int dwDesiredAccess, int bInheritHandle, int dwOptions) {
        if (duplicateHandle == null) {
            duplicateHandle = linker.downcallHandle(
                    kernel32.find("DuplicateHandle").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(duplicateHandle, hSourceProcessHandle, hSourceHandle, hTargetProcessHandle, lpTargetHandle, dwDesiredAccess, bInheritHandle, dwOptions);
    }

    // =====================================================================
    // SetLastError
    // =====================================================================
    private static @Nullable MethodHandle setLastError;
    public static void SetLastError(int dwErrCode) {
        if (setLastError == null) {
            setLastError = linker.downcallHandle(
                    kernel32.find("SetLastError").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)
            );
        }

        invoke(setLastError, dwErrCode);
    }

    // =====================================================================
    // FormatMessageW
    // =====================================================================
    private static @Nullable MethodHandle formatMessageW;
    public static int FormatMessageW(int dwFlags, MemorySegment lpSource, int dwMessageId, int dwLanguageId, MemorySegment lpBuffer, int nSize, MemorySegment Arguments) {
        if (formatMessageW == null) {
            formatMessageW = linker.downcallHandle(
                    kernel32.find("FormatMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(formatMessageW, dwFlags, lpSource, dwMessageId, dwLanguageId, lpBuffer, nSize, Arguments);
    }

    // =====================================================================
    // OutputDebugStringW
    // =====================================================================
    private static @Nullable MethodHandle outputDebugStringW;
    public static void OutputDebugStringW(MemorySegment lpOutputString) {
        if (outputDebugStringW == null) {
            outputDebugStringW = linker.downcallHandle(
                    kernel32.find("OutputDebugStringW").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(outputDebugStringW, lpOutputString);
    }

    // =====================================================================
    // IsDebuggerPresent
    // =====================================================================
    private static @Nullable MethodHandle isDebuggerPresent;
    public static int IsDebuggerPresent() {
        if (isDebuggerPresent == null) {
            isDebuggerPresent = linker.downcallHandle(
                    kernel32.find("IsDebuggerPresent").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(isDebuggerPresent);
    }

    // =====================================================================
    // DebugBreak
    // =====================================================================
    private static @Nullable MethodHandle debugBreak;
    public static void DebugBreak() {
        if (debugBreak == null) {
            debugBreak = linker.downcallHandle(
                    kernel32.find("DebugBreak").orElseThrow(),
                    FunctionDescriptor.ofVoid()
            );
        }

        invoke(debugBreak);
    }

    // =====================================================================
    // ExitProcess
    // =====================================================================
    private static @Nullable MethodHandle exitProcess;
    public static void ExitProcess(int uExitCode) {
        if (exitProcess == null) {
            exitProcess = linker.downcallHandle(
                    kernel32.find("ExitProcess").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)
            );
        }

        invoke(exitProcess, uExitCode);
    }

    // =====================================================================
    // GetCommandLineW
    // =====================================================================
    private static @Nullable MethodHandle getCommandLineW;
    public static MemorySegment GetCommandLineW() {
        if (getCommandLineW == null) {
            getCommandLineW = linker.downcallHandle(
                    kernel32.find("GetCommandLineW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getCommandLineW);
    }

    // =====================================================================
    // GetStartupInfoW
    // =====================================================================
    private static @Nullable MethodHandle getStartupInfoW;
    public static void GetStartupInfoW(MemorySegment lpStartupInfo) {
        if (getStartupInfoW == null) {
            getStartupInfoW = linker.downcallHandle(
                    kernel32.find("GetStartupInfoW").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
        }

        invoke(getStartupInfoW, lpStartupInfo);
    }

    // =====================================================================
    // SetHandleInformation
    // =====================================================================
    private static @Nullable MethodHandle setHandleInformation;
    public static int SetHandleInformation(MemorySegment hObject, int dwMask, int dwFlags) {
        if (setHandleInformation == null) {
            setHandleInformation = linker.downcallHandle(
                    kernel32.find("SetHandleInformation").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setHandleInformation, hObject, dwMask, dwFlags);
    }

    // =====================================================================
    // GetHandleInformation
    // =====================================================================
    private static @Nullable MethodHandle getHandleInformation;
    public static int GetHandleInformation(MemorySegment hObject, MemorySegment lpdwFlags) {
        if (getHandleInformation == null) {
            getHandleInformation = linker.downcallHandle(
                    kernel32.find("GetHandleInformation").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getHandleInformation, hObject, lpdwFlags);
    }

    // =====================================================================
    // MultiByteToWideChar
    // =====================================================================
    private static @Nullable MethodHandle multiByteToWideChar;
    public static int MultiByteToWideChar(int CodePage, int dwFlags, MemorySegment lpMultiByteStr, int cbMultiByte, MemorySegment lpWideCharStr, int cchWideChar) {
        if (multiByteToWideChar == null) {
            multiByteToWideChar = linker.downcallHandle(
                    kernel32.find("MultiByteToWideChar").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(multiByteToWideChar, CodePage, dwFlags, lpMultiByteStr, cbMultiByte, lpWideCharStr, cchWideChar);
    }

    // =====================================================================
    // WideCharToMultiByte
    // =====================================================================
    private static @Nullable MethodHandle wideCharToMultiByte;
    public static int WideCharToMultiByte(int CodePage, int dwFlags, MemorySegment lpWideCharStr, int cchWideChar, MemorySegment lpMultiByteStr, int cbMultiByte, MemorySegment lpDefaultChar, MemorySegment lpUsedDefaultChar) {
        if (wideCharToMultiByte == null) {
            wideCharToMultiByte = linker.downcallHandle(
                    kernel32.find("WideCharToMultiByte").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(wideCharToMultiByte, CodePage, dwFlags, lpWideCharStr, cchWideChar, lpMultiByteStr, cbMultiByte, lpDefaultChar, lpUsedDefaultChar);
    }

    // =====================================================================
    // GetDiskFreeSpaceExW
    // =====================================================================
    private static @Nullable MethodHandle getDiskFreeSpaceExW;
    public static int GetDiskFreeSpaceExW(MemorySegment lpDirectoryName, MemorySegment lpFreeBytesAvailableToCaller, MemorySegment lpTotalNumberOfBytes, MemorySegment lpTotalNumberOfFreeBytes) {
        if (getDiskFreeSpaceExW == null) {
            getDiskFreeSpaceExW = linker.downcallHandle(
                    kernel32.find("GetDiskFreeSpaceExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getDiskFreeSpaceExW, lpDirectoryName, lpFreeBytesAvailableToCaller, lpTotalNumberOfBytes, lpTotalNumberOfFreeBytes);
    }

    // =====================================================================
    // GetDriveTypeW
    // =====================================================================
    private static @Nullable MethodHandle getDriveTypeW;
    public static int GetDriveTypeW(MemorySegment lpRootPathName) {
        if (getDriveTypeW == null) {
            getDriveTypeW = linker.downcallHandle(
                    kernel32.find("GetDriveTypeW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getDriveTypeW, lpRootPathName);
    }

    // =====================================================================
    // GetLogicalDrives
    // =====================================================================
    private static @Nullable MethodHandle getLogicalDrives;
    public static int GetLogicalDrives() {
        if (getLogicalDrives == null) {
            getLogicalDrives = linker.downcallHandle(
                    kernel32.find("GetLogicalDrives").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getLogicalDrives);
    }

    // =====================================================================
    // GetLogicalDriveStringsW
    // =====================================================================
    private static @Nullable MethodHandle getLogicalDriveStringsW;
    public static int GetLogicalDriveStringsW(int nBufferLength, MemorySegment lpBuffer) {
        if (getLogicalDriveStringsW == null) {
            getLogicalDriveStringsW = linker.downcallHandle(
                    kernel32.find("GetLogicalDriveStringsW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getLogicalDriveStringsW, nBufferLength, lpBuffer);
    }

    // =====================================================================
    // GetVolumeInformationW
    // =====================================================================
    private static @Nullable MethodHandle getVolumeInformationW;
    public static int GetVolumeInformationW(MemorySegment lpRootPathName, MemorySegment lpVolumeNameBuffer, int nVolumeNameSize, MemorySegment lpVolumeSerialNumber, MemorySegment lpMaximumComponentLength, MemorySegment lpFileSystemFlags, MemorySegment lpFileSystemNameBuffer, int nFileSystemNameSize) {
        if (getVolumeInformationW == null) {
            getVolumeInformationW = linker.downcallHandle(
                    kernel32.find("GetVolumeInformationW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getVolumeInformationW, lpRootPathName, lpVolumeNameBuffer, nVolumeNameSize, lpVolumeSerialNumber, lpMaximumComponentLength, lpFileSystemFlags, lpFileSystemNameBuffer, nFileSystemNameSize);
    }

    // =====================================================================
    // TlsAlloc
    // =====================================================================
    private static @Nullable MethodHandle tlsAlloc;
    public static int TlsAlloc() {
        if (tlsAlloc == null) {
            tlsAlloc = linker.downcallHandle(
                    kernel32.find("TlsAlloc").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(tlsAlloc);
    }

    // =====================================================================
    // TlsFree
    // =====================================================================
    private static @Nullable MethodHandle tlsFree;
    public static int TlsFree(int dwTlsIndex) {
        if (tlsFree == null) {
            tlsFree = linker.downcallHandle(
                    kernel32.find("TlsFree").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(tlsFree, dwTlsIndex);
    }

    // =====================================================================
    // TlsGetValue
    // =====================================================================
    private static @Nullable MethodHandle tlsGetValue;
    public static MemorySegment TlsGetValue(int dwTlsIndex) {
        if (tlsGetValue == null) {
            tlsGetValue = linker.downcallHandle(
                    kernel32.find("TlsGetValue").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(tlsGetValue, dwTlsIndex);
    }

    // =====================================================================
    // TlsSetValue
    // =====================================================================
    private static @Nullable MethodHandle tlsSetValue;
    public static int TlsSetValue(int dwTlsIndex, MemorySegment lpTlsValue) {
        if (tlsSetValue == null) {
            tlsSetValue = linker.downcallHandle(
                    kernel32.find("TlsSetValue").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(tlsSetValue, dwTlsIndex, lpTlsValue);
    }

    // =====================================================================
    // CreateFileMappingW
    // =====================================================================
    private static @Nullable MethodHandle createFileMappingW;
    public static MemorySegment CreateFileMappingW(MemorySegment hFile, MemorySegment lpFileMappingAttributes, int flProtect, int dwMaximumSizeHigh, int dwMaximumSizeLow, MemorySegment lpName) {
        if (createFileMappingW == null) {
            createFileMappingW = linker.downcallHandle(
                    kernel32.find("CreateFileMappingW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createFileMappingW, hFile, lpFileMappingAttributes, flProtect, dwMaximumSizeHigh, dwMaximumSizeLow, lpName);
    }

    // =====================================================================
    // OpenFileMappingW
    // =====================================================================
    private static @Nullable MethodHandle openFileMappingW;
    public static MemorySegment OpenFileMappingW(int dwDesiredAccess, int bInheritHandle, MemorySegment lpName) {
        if (openFileMappingW == null) {
            openFileMappingW = linker.downcallHandle(
                    kernel32.find("OpenFileMappingW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(openFileMappingW, dwDesiredAccess, bInheritHandle, lpName);
    }

    // =====================================================================
    // MapViewOfFile
    // =====================================================================
    private static @Nullable MethodHandle mapViewOfFile;
    public static MemorySegment MapViewOfFile(MemorySegment hFileMappingObject, int dwDesiredAccess, int dwFileOffsetHigh, int dwFileOffsetLow, long dwNumberOfBytesToMap) {
        if (mapViewOfFile == null) {
            mapViewOfFile = linker.downcallHandle(
                    kernel32.find("MapViewOfFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(mapViewOfFile, hFileMappingObject, dwDesiredAccess, dwFileOffsetHigh, dwFileOffsetLow, dwNumberOfBytesToMap);
    }

    // =====================================================================
    // UnmapViewOfFile
    // =====================================================================
    private static @Nullable MethodHandle unmapViewOfFile;
    public static int UnmapViewOfFile(MemorySegment lpBaseAddress) {
        if (unmapViewOfFile == null) {
            unmapViewOfFile = linker.downcallHandle(
                    kernel32.find("UnmapViewOfFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(unmapViewOfFile, lpBaseAddress);
    }

    // =====================================================================
    // LockFile
    // =====================================================================
    private static @Nullable MethodHandle lockFile;
    public static int LockFile(MemorySegment hFile, int dwFileOffsetLow, int dwFileOffsetHigh, int nNumberOfBytesToLockLow, int nNumberOfBytesToLockHigh) {
        if (lockFile == null) {
            lockFile = linker.downcallHandle(
                    kernel32.find("LockFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(lockFile, hFile, dwFileOffsetLow, dwFileOffsetHigh, nNumberOfBytesToLockLow, nNumberOfBytesToLockHigh);
    }

    // =====================================================================
    // UnlockFile
    // =====================================================================
    private static @Nullable MethodHandle unlockFile;
    public static int UnlockFile(MemorySegment hFile, int dwFileOffsetLow, int dwFileOffsetHigh, int nNumberOfBytesToUnlockLow, int nNumberOfBytesToUnlockHigh) {
        if (unlockFile == null) {
            unlockFile = linker.downcallHandle(
                    kernel32.find("UnlockFile").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(unlockFile, hFile, dwFileOffsetLow, dwFileOffsetHigh, nNumberOfBytesToUnlockLow, nNumberOfBytesToUnlockHigh);
    }

    // =====================================================================
    // LockFileEx
    // =====================================================================
    private static @Nullable MethodHandle lockFileEx;
    public static int LockFileEx(MemorySegment hFile, int dwFlags, int dwReserved, int nNumberOfBytesToLockLow, int nNumberOfBytesToLockHigh, MemorySegment lpOverlapped) {
        if (lockFileEx == null) {
            lockFileEx = linker.downcallHandle(
                    kernel32.find("LockFileEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(lockFileEx, hFile, dwFlags, dwReserved, nNumberOfBytesToLockLow, nNumberOfBytesToLockHigh, lpOverlapped);
    }

    // =====================================================================
    // UnlockFileEx
    // =====================================================================
    private static @Nullable MethodHandle unlockFileEx;
    public static int UnlockFileEx(MemorySegment hFile, int dwReserved, int nNumberOfBytesToUnlockLow, int nNumberOfBytesToUnlockHigh, MemorySegment lpOverlapped) {
        if (unlockFileEx == null) {
            unlockFileEx = linker.downcallHandle(
                    kernel32.find("UnlockFileEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(unlockFileEx, hFile, dwReserved, nNumberOfBytesToUnlockLow, nNumberOfBytesToUnlockHigh, lpOverlapped);
    }

    // =====================================================================
    // WriteConsoleOutputCharacterW
    // =====================================================================
    private static @Nullable MethodHandle writeConsoleOutputCharacterW;
    public static int WriteConsoleOutputCharacterW(MemorySegment hConsoleOutput, MemorySegment lpCharacter, int nLength, int dwWriteCoord, MemorySegment lpNumberOfCharsWritten) {
        if (writeConsoleOutputCharacterW == null) {
            writeConsoleOutputCharacterW = linker.downcallHandle(
                    kernel32.find("WriteConsoleOutputCharacterW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(writeConsoleOutputCharacterW, hConsoleOutput, lpCharacter, nLength, dwWriteCoord, lpNumberOfCharsWritten);
    }

    // =====================================================================
    // WriteConsoleOutputAttribute
    // =====================================================================
    private static @Nullable MethodHandle writeConsoleOutputAttribute;
    public static int WriteConsoleOutputAttribute(MemorySegment hConsoleOutput, MemorySegment lpAttribute, int nLength, int dwWriteCoord, MemorySegment lpNumberOfAttrsWritten) {
        if (writeConsoleOutputAttribute == null) {
            writeConsoleOutputAttribute = linker.downcallHandle(
                    kernel32.find("WriteConsoleOutputAttribute").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(writeConsoleOutputAttribute, hConsoleOutput, lpAttribute, nLength, dwWriteCoord, lpNumberOfAttrsWritten);
    }

    // =====================================================================
    // ReadConsoleOutputCharacterW
    // =====================================================================
    private static @Nullable MethodHandle readConsoleOutputCharacterW;
    public static int ReadConsoleOutputCharacterW(MemorySegment hConsoleOutput, MemorySegment lpCharacter, int nLength, int dwReadCoord, MemorySegment lpNumberOfCharsRead) {
        if (readConsoleOutputCharacterW == null) {
            readConsoleOutputCharacterW = linker.downcallHandle(
                    kernel32.find("ReadConsoleOutputCharacterW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(readConsoleOutputCharacterW, hConsoleOutput, lpCharacter, nLength, dwReadCoord, lpNumberOfCharsRead);
    }

    // =====================================================================
    // ReadConsoleOutputAttribute
    // =====================================================================
    private static @Nullable MethodHandle readConsoleOutputAttribute;
    public static int ReadConsoleOutputAttribute(MemorySegment hConsoleOutput, MemorySegment lpAttribute, int nLength, int dwReadCoord, MemorySegment lpNumberOfAttrsRead) {
        if (readConsoleOutputAttribute == null) {
            readConsoleOutputAttribute = linker.downcallHandle(
                    kernel32.find("ReadConsoleOutputAttribute").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(readConsoleOutputAttribute, hConsoleOutput, lpAttribute, nLength, dwReadCoord, lpNumberOfAttrsRead);
    }

    // =====================================================================
    // WriteConsoleOutputW
    // =====================================================================
    private static @Nullable MethodHandle writeConsoleOutputW;
    public static int WriteConsoleOutputW(MemorySegment hConsoleOutput, MemorySegment lpBuffer, int dwBufferSize, int dwBufferCoord, MemorySegment lpWriteRegion) {
        if (writeConsoleOutputW == null) {
            writeConsoleOutputW = linker.downcallHandle(
                    kernel32.find("WriteConsoleOutputW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(writeConsoleOutputW, hConsoleOutput, lpBuffer, dwBufferSize, dwBufferCoord, lpWriteRegion);
    }

    // =====================================================================
    // ReadConsoleOutputW
    // =====================================================================
    private static @Nullable MethodHandle readConsoleOutputW;
    public static int ReadConsoleOutputW(MemorySegment hConsoleOutput, MemorySegment lpBuffer, int dwBufferSize, int dwBufferCoord, MemorySegment lpReadRegion) {
        if (readConsoleOutputW == null) {
            readConsoleOutputW = linker.downcallHandle(
                    kernel32.find("ReadConsoleOutputW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(readConsoleOutputW, hConsoleOutput, lpBuffer, dwBufferSize, dwBufferCoord, lpReadRegion);
    }

    // =====================================================================
    // AllocConsole
    // =====================================================================
    private static @Nullable MethodHandle allocConsole;
    public static int AllocConsole() {
        if (allocConsole == null) {
            allocConsole = linker.downcallHandle(
                    kernel32.find("AllocConsole").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(allocConsole);
    }

    // =====================================================================
    // FreeConsole
    // =====================================================================
    private static @Nullable MethodHandle freeConsole;
    public static int FreeConsole() {
        if (freeConsole == null) {
            freeConsole = linker.downcallHandle(
                    kernel32.find("FreeConsole").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(freeConsole);
    }

    // =====================================================================
    // AttachConsole
    // =====================================================================
    private static @Nullable MethodHandle attachConsole;
    public static int AttachConsole(int dwProcessId) {
        if (attachConsole == null) {
            attachConsole = linker.downcallHandle(
                    kernel32.find("AttachConsole").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(attachConsole, dwProcessId);
    }

    // =====================================================================
    // GetConsoleWindow
    // =====================================================================
    private static @Nullable MethodHandle getConsoleWindow;
    public static MemorySegment GetConsoleWindow() {
        if (getConsoleWindow == null) {
            getConsoleWindow = linker.downcallHandle(
                    kernel32.find("GetConsoleWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getConsoleWindow);
    }

    // =====================================================================
    // SetConsoleCtrlHandler
    // =====================================================================
    private static @Nullable MethodHandle setConsoleCtrlHandler;
    public static int SetConsoleCtrlHandler(MemorySegment HandlerRoutine, int Add) {
        if (setConsoleCtrlHandler == null) {
            setConsoleCtrlHandler = linker.downcallHandle(
                    kernel32.find("SetConsoleCtrlHandler").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setConsoleCtrlHandler, HandlerRoutine, Add);
    }

    // =====================================================================
    // GenerateConsoleCtrlEvent
    // =====================================================================
    private static @Nullable MethodHandle generateConsoleCtrlEvent;
    public static int GenerateConsoleCtrlEvent(int dwCtrlEvent, int dwProcessGroupId) {
        if (generateConsoleCtrlEvent == null) {
            generateConsoleCtrlEvent = linker.downcallHandle(
                    kernel32.find("GenerateConsoleCtrlEvent").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(generateConsoleCtrlEvent, dwCtrlEvent, dwProcessGroupId);
    }

    // =====================================================================
    // SetThreadPriority
    // =====================================================================
    private static @Nullable MethodHandle setThreadPriority;
    public static int SetThreadPriority(MemorySegment hThread, int nPriority) {
        if (setThreadPriority == null) {
            setThreadPriority = linker.downcallHandle(
                    kernel32.find("SetThreadPriority").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setThreadPriority, hThread, nPriority);
    }

    // =====================================================================
    // GetThreadPriority
    // =====================================================================
    private static @Nullable MethodHandle getThreadPriority;
    public static int GetThreadPriority(MemorySegment hThread) {
        if (getThreadPriority == null) {
            getThreadPriority = linker.downcallHandle(
                    kernel32.find("GetThreadPriority").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getThreadPriority, hThread);
    }

    // =====================================================================
    // SetPriorityClass
    // =====================================================================
    private static @Nullable MethodHandle setPriorityClass;
    public static int SetPriorityClass(MemorySegment hProcess, int dwPriorityClass) {
        if (setPriorityClass == null) {
            setPriorityClass = linker.downcallHandle(
                    kernel32.find("SetPriorityClass").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setPriorityClass, hProcess, dwPriorityClass);
    }

    // =====================================================================
    // GetPriorityClass
    // =====================================================================
    private static @Nullable MethodHandle getPriorityClass;
    public static int GetPriorityClass(MemorySegment hProcess) {
        if (getPriorityClass == null) {
            getPriorityClass = linker.downcallHandle(
                    kernel32.find("GetPriorityClass").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getPriorityClass, hProcess);
    }

    // =====================================================================
    // Beep
    // =====================================================================
    private static @Nullable MethodHandle beep;
    public static int Beep(int dwFreq, int dwDuration) {
        if (beep == null) {
            beep = linker.downcallHandle(
                    kernel32.find("Beep").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(beep, dwFreq, dwDuration);
    }

    // =====================================================================
    // GetProcessTimes
    // =====================================================================
    private static @Nullable MethodHandle getProcessTimes;
    public static int GetProcessTimes(MemorySegment hProcess, MemorySegment lpCreationTime, MemorySegment lpExitTime, MemorySegment lpKernelTime, MemorySegment lpUserTime) {
        if (getProcessTimes == null) {
            getProcessTimes = linker.downcallHandle(
                    kernel32.find("GetProcessTimes").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getProcessTimes, hProcess, lpCreationTime, lpExitTime, lpKernelTime, lpUserTime);
    }

    // =====================================================================
    // GetThreadTimes
    // =====================================================================
    private static @Nullable MethodHandle getThreadTimes;
    public static int GetThreadTimes(MemorySegment hThread, MemorySegment lpCreationTime, MemorySegment lpExitTime, MemorySegment lpKernelTime, MemorySegment lpUserTime) {
        if (getThreadTimes == null) {
            getThreadTimes = linker.downcallHandle(
                    kernel32.find("GetThreadTimes").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getThreadTimes, hThread, lpCreationTime, lpExitTime, lpKernelTime, lpUserTime);
    }

    // =====================================================================
    // GetFileTime
    // =====================================================================
    private static @Nullable MethodHandle getFileTime;
    public static int GetFileTime(MemorySegment hFile, MemorySegment lpCreationTime, MemorySegment lpLastAccessTime, MemorySegment lpLastWriteTime) {
        if (getFileTime == null) {
            getFileTime = linker.downcallHandle(
                    kernel32.find("GetFileTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getFileTime, hFile, lpCreationTime, lpLastAccessTime, lpLastWriteTime);
    }

    // =====================================================================
    // SetFileTime
    // =====================================================================
    private static @Nullable MethodHandle setFileTime;
    public static int SetFileTime(MemorySegment hFile, MemorySegment lpCreationTime, MemorySegment lpLastAccessTime, MemorySegment lpLastWriteTime) {
        if (setFileTime == null) {
            setFileTime = linker.downcallHandle(
                    kernel32.find("SetFileTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setFileTime, hFile, lpCreationTime, lpLastAccessTime, lpLastWriteTime);
    }

    // =====================================================================
    // CompareFileTime
    // =====================================================================
    private static @Nullable MethodHandle compareFileTime;
    public static int CompareFileTime(MemorySegment lpFileTime1, MemorySegment lpFileTime2) {
        if (compareFileTime == null) {
            compareFileTime = linker.downcallHandle(
                    kernel32.find("CompareFileTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(compareFileTime, lpFileTime1, lpFileTime2);
    }

    // =====================================================================
    // FileTimeToLocalFileTime
    // =====================================================================
    private static @Nullable MethodHandle fileTimeToLocalFileTime;
    public static int FileTimeToLocalFileTime(MemorySegment lpFileTime, MemorySegment lpLocalFileTime) {
        if (fileTimeToLocalFileTime == null) {
            fileTimeToLocalFileTime = linker.downcallHandle(
                    kernel32.find("FileTimeToLocalFileTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(fileTimeToLocalFileTime, lpFileTime, lpLocalFileTime);
    }

    // =====================================================================
    // LocalFileTimeToFileTime
    // =====================================================================
    private static @Nullable MethodHandle localFileTimeToFileTime;
    public static int LocalFileTimeToFileTime(MemorySegment lpLocalFileTime, MemorySegment lpFileTime) {
        if (localFileTimeToFileTime == null) {
            localFileTimeToFileTime = linker.downcallHandle(
                    kernel32.find("LocalFileTimeToFileTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(localFileTimeToFileTime, lpLocalFileTime, lpFileTime);
    }

    // =====================================================================
    // GetOverlappedResult
    // =====================================================================
    private static @Nullable MethodHandle getOverlappedResult;
    public static int GetOverlappedResult(MemorySegment hFile, MemorySegment lpOverlapped, MemorySegment lpNumberOfBytesTransferred, int bWait) {
        if (getOverlappedResult == null) {
            getOverlappedResult = linker.downcallHandle(
                    kernel32.find("GetOverlappedResult").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getOverlappedResult, hFile, lpOverlapped, lpNumberOfBytesTransferred, bWait);
    }

    // =====================================================================
    // CancelIo
    // =====================================================================
    private static @Nullable MethodHandle cancelIo;
    public static int CancelIo(MemorySegment hFile) {
        if (cancelIo == null) {
            cancelIo = linker.downcallHandle(
                    kernel32.find("CancelIo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(cancelIo, hFile);
    }

    // =====================================================================
    // CancelIoEx
    // =====================================================================
    private static @Nullable MethodHandle cancelIoEx;
    public static int CancelIoEx(MemorySegment hFile, MemorySegment lpOverlapped) {
        if (cancelIoEx == null) {
            cancelIoEx = linker.downcallHandle(
                    kernel32.find("CancelIoEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(cancelIoEx, hFile, lpOverlapped);
    }

    // =====================================================================
    // DeviceIoControl
    // =====================================================================
    private static @Nullable MethodHandle deviceIoControl;
    public static int DeviceIoControl(MemorySegment hDevice, int dwIoControlCode, MemorySegment lpInBuffer, int nInBufferSize, MemorySegment lpOutBuffer, int nOutBufferSize, MemorySegment lpBytesReturned, MemorySegment lpOverlapped) {
        if (deviceIoControl == null) {
            deviceIoControl = linker.downcallHandle(
                    kernel32.find("DeviceIoControl").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(deviceIoControl, hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped);
    }

    // =====================================================================
    // Helper: Pack COORD into int (X in low word, Y in high word)
    // =====================================================================
    public static int packCoord(short x, short y) {
        return (y << 16) | (x & 0xFFFF);
    }

    // =====================================================================
    // Helper: Unpack X from COORD int
    // =====================================================================
    public static short unpackCoordX(int coord) {
        return (short) (coord & 0xFFFF);
    }

    // =====================================================================
    // Helper: Unpack Y from COORD int
    // =====================================================================
    public static short unpackCoordY(int coord) {
        return (short) ((coord >> 16) & 0xFFFF);
    }
}
