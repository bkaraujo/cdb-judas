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
 * User32.dll Windows API functions for user interface operations.
 *
 * Categories:
 *   - Window: CreateWindowExW, DestroyWindow, ShowWindow, FindWindowW, GetForegroundWindow, etc.
 *   - Message: GetMessageW, PeekMessageW, TranslateMessage, DispatchMessageW, SendMessageW, PostMessageW, etc.
 *   - Input: GetKeyState, GetAsyncKeyState, GetCursorPos, SetCursorPos, keybd_event, mouse_event, etc.
 *   - Clipboard: OpenClipboard, CloseClipboard, GetClipboardData, SetClipboardData, etc.
 *   - Painting: GetDC, ReleaseDC, BeginPaint, EndPaint, InvalidateRect, etc.
 *   - Dialog: MessageBoxW, CreateDialogW, DialogBoxW, GetDlgItem, etc.
 *   - Menu: CreateMenu, CreatePopupMenu, DestroyMenu, AppendMenuW, TrackPopupMenu, etc.
 *   - Hook: SetWindowsHookExW, UnhookWindowsHookEx, CallNextHookEx
 *   - Timer: SetTimer, KillTimer
 *   - System: GetSystemMetrics, SystemParametersInfoW, GetDesktopWindow, etc.
 *   - Class: RegisterClassExW, UnregisterClassW, GetClassInfoExW
 *   - Caret: CreateCaret, DestroyCaret, ShowCaret, HideCaret, SetCaretPos, GetCaretPos
 *   - Cursor/Icon: LoadCursorW, LoadIconW, SetCursor, GetCursor, DestroyCursor, DestroyIcon
 */
@NullMarked
public abstract class User32 {
    private User32(){}

    private static final SymbolLookup user32 = SymbolLookup.libraryLookup("user32", arena);

    // =====================================================================
    // MessageBoxW
    // =====================================================================
    private static @Nullable MethodHandle messageBoxW;
    public static int MessageBoxW(MemorySegment hWnd, MemorySegment lpText, MemorySegment lpCaption, int uType) {
        if (messageBoxW == null) {
            messageBoxW = linker.downcallHandle(
                    user32.find("MessageBoxW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(messageBoxW, hWnd, lpText, lpCaption, uType);
    }

    // =====================================================================
    // CreateWindowExW
    // =====================================================================
    private static @Nullable MethodHandle createWindowExW;
    public static MemorySegment CreateWindowExW(int dwExStyle, MemorySegment lpClassName, MemorySegment lpWindowName, int dwStyle, int x, int y, int nWidth, int nHeight, MemorySegment hWndParent, MemorySegment hMenu, MemorySegment hInstance, MemorySegment lpParam) {
        if (createWindowExW == null) {
            createWindowExW = linker.downcallHandle(
                    user32.find("CreateWindowExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(createWindowExW, dwExStyle, lpClassName, lpWindowName, dwStyle, x, y, nWidth, nHeight, hWndParent, hMenu, hInstance, lpParam);
    }

    // =====================================================================
    // DestroyWindow
    // =====================================================================
    private static @Nullable MethodHandle destroyWindow;
    public static int DestroyWindow(MemorySegment hWnd) {
        if (destroyWindow == null) {
            destroyWindow = linker.downcallHandle(
                    user32.find("DestroyWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(destroyWindow, hWnd);
    }

    // =====================================================================
    // ShowWindow
    // =====================================================================
    private static @Nullable MethodHandle showWindow;
    public static int ShowWindow(MemorySegment hWnd, int nCmdShow) {
        if (showWindow == null) {
            showWindow = linker.downcallHandle(
                    user32.find("ShowWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(showWindow, hWnd, nCmdShow);
    }

    // =====================================================================
    // UpdateWindow
    // =====================================================================
    private static @Nullable MethodHandle updateWindow;
    public static int UpdateWindow(MemorySegment hWnd) {
        if (updateWindow == null) {
            updateWindow = linker.downcallHandle(
                    user32.find("UpdateWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(updateWindow, hWnd);
    }

    // =====================================================================
    // SetWindowPos
    // =====================================================================
    private static @Nullable MethodHandle setWindowPos;
    public static int SetWindowPos(MemorySegment hWnd, MemorySegment hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags) {
        if (setWindowPos == null) {
            setWindowPos = linker.downcallHandle(
                    user32.find("SetWindowPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setWindowPos, hWnd, hWndInsertAfter, X, Y, cx, cy, uFlags);
    }

    // =====================================================================
    // MoveWindow
    // =====================================================================
    private static @Nullable MethodHandle moveWindow;
    public static int MoveWindow(MemorySegment hWnd, int X, int Y, int nWidth, int nHeight, int bRepaint) {
        if (moveWindow == null) {
            moveWindow = linker.downcallHandle(
                    user32.find("MoveWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(moveWindow, hWnd, X, Y, nWidth, nHeight, bRepaint);
    }

    // =====================================================================
    // GetWindowRect
    // =====================================================================
    private static @Nullable MethodHandle getWindowRect;
    public static int GetWindowRect(MemorySegment hWnd, MemorySegment lpRect) {
        if (getWindowRect == null) {
            getWindowRect = linker.downcallHandle(
                    user32.find("GetWindowRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getWindowRect, hWnd, lpRect);
    }

    // =====================================================================
    // GetClientRect
    // =====================================================================
    private static @Nullable MethodHandle getClientRect;
    public static int GetClientRect(MemorySegment hWnd, MemorySegment lpRect) {
        if (getClientRect == null) {
            getClientRect = linker.downcallHandle(
                    user32.find("GetClientRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getClientRect, hWnd, lpRect);
    }

    // =====================================================================
    // GetWindowTextW
    // =====================================================================
    private static @Nullable MethodHandle getWindowTextW;
    public static int GetWindowTextW(MemorySegment hWnd, MemorySegment lpString, int nMaxCount) {
        if (getWindowTextW == null) {
            getWindowTextW = linker.downcallHandle(
                    user32.find("GetWindowTextW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getWindowTextW, hWnd, lpString, nMaxCount);
    }

    // =====================================================================
    // SetWindowTextW
    // =====================================================================
    private static @Nullable MethodHandle setWindowTextW;
    public static int SetWindowTextW(MemorySegment hWnd, MemorySegment lpString) {
        if (setWindowTextW == null) {
            setWindowTextW = linker.downcallHandle(
                    user32.find("SetWindowTextW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setWindowTextW, hWnd, lpString);
    }

    // =====================================================================
    // GetWindowTextLengthW
    // =====================================================================
    private static @Nullable MethodHandle getWindowTextLengthW;
    public static int GetWindowTextLengthW(MemorySegment hWnd) {
        if (getWindowTextLengthW == null) {
            getWindowTextLengthW = linker.downcallHandle(
                    user32.find("GetWindowTextLengthW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getWindowTextLengthW, hWnd);
    }

    // =====================================================================
    // FindWindowW
    // =====================================================================
    private static @Nullable MethodHandle findWindowW;
    public static MemorySegment FindWindowW(MemorySegment lpClassName, MemorySegment lpWindowName) {
        if (findWindowW == null) {
            findWindowW = linker.downcallHandle(
                    user32.find("FindWindowW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(findWindowW, lpClassName, lpWindowName);
    }

    // =====================================================================
    // FindWindowExW
    // =====================================================================
    private static @Nullable MethodHandle findWindowExW;
    public static MemorySegment FindWindowExW(MemorySegment hWndParent, MemorySegment hWndChildAfter, MemorySegment lpszClass, MemorySegment lpszWindow) {
        if (findWindowExW == null) {
            findWindowExW = linker.downcallHandle(
                    user32.find("FindWindowExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(findWindowExW, hWndParent, hWndChildAfter, lpszClass, lpszWindow);
    }

    // =====================================================================
    // GetForegroundWindow
    // =====================================================================
    private static @Nullable MethodHandle getForegroundWindow;
    public static MemorySegment GetForegroundWindow() {
        if (getForegroundWindow == null) {
            getForegroundWindow = linker.downcallHandle(
                    user32.find("GetForegroundWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getForegroundWindow);
    }

    // =====================================================================
    // SetForegroundWindow
    // =====================================================================
    private static @Nullable MethodHandle setForegroundWindow;
    public static int SetForegroundWindow(MemorySegment hWnd) {
        if (setForegroundWindow == null) {
            setForegroundWindow = linker.downcallHandle(
                    user32.find("SetForegroundWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setForegroundWindow, hWnd);
    }

    // =====================================================================
    // GetDesktopWindow
    // =====================================================================
    private static @Nullable MethodHandle getDesktopWindow;
    public static MemorySegment GetDesktopWindow() {
        if (getDesktopWindow == null) {
            getDesktopWindow = linker.downcallHandle(
                    user32.find("GetDesktopWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getDesktopWindow);
    }

    // =====================================================================
    // GetActiveWindow
    // =====================================================================
    private static @Nullable MethodHandle getActiveWindow;
    public static MemorySegment GetActiveWindow() {
        if (getActiveWindow == null) {
            getActiveWindow = linker.downcallHandle(
                    user32.find("GetActiveWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getActiveWindow);
    }

    // =====================================================================
    // SetActiveWindow
    // =====================================================================
    private static @Nullable MethodHandle setActiveWindow;
    public static MemorySegment SetActiveWindow(MemorySegment hWnd) {
        if (setActiveWindow == null) {
            setActiveWindow = linker.downcallHandle(
                    user32.find("SetActiveWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setActiveWindow, hWnd);
    }

    // =====================================================================
    // GetFocus
    // =====================================================================
    private static @Nullable MethodHandle getFocus;
    public static MemorySegment GetFocus() {
        if (getFocus == null) {
            getFocus = linker.downcallHandle(
                    user32.find("GetFocus").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getFocus);
    }

    // =====================================================================
    // SetFocus
    // =====================================================================
    private static @Nullable MethodHandle setFocus;
    public static MemorySegment SetFocus(MemorySegment hWnd) {
        if (setFocus == null) {
            setFocus = linker.downcallHandle(
                    user32.find("SetFocus").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setFocus, hWnd);
    }

    // =====================================================================
    // EnableWindow
    // =====================================================================
    private static @Nullable MethodHandle enableWindow;
    public static int EnableWindow(MemorySegment hWnd, int bEnable) {
        if (enableWindow == null) {
            enableWindow = linker.downcallHandle(
                    user32.find("EnableWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(enableWindow, hWnd, bEnable);
    }

    // =====================================================================
    // IsWindow
    // =====================================================================
    private static @Nullable MethodHandle isWindow;
    public static int IsWindow(MemorySegment hWnd) {
        if (isWindow == null) {
            isWindow = linker.downcallHandle(
                    user32.find("IsWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(isWindow, hWnd);
    }

    // =====================================================================
    // IsWindowVisible
    // =====================================================================
    private static @Nullable MethodHandle isWindowVisible;
    public static int IsWindowVisible(MemorySegment hWnd) {
        if (isWindowVisible == null) {
            isWindowVisible = linker.downcallHandle(
                    user32.find("IsWindowVisible").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(isWindowVisible, hWnd);
    }

    // =====================================================================
    // IsWindowEnabled
    // =====================================================================
    private static @Nullable MethodHandle isWindowEnabled;
    public static int IsWindowEnabled(MemorySegment hWnd) {
        if (isWindowEnabled == null) {
            isWindowEnabled = linker.downcallHandle(
                    user32.find("IsWindowEnabled").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(isWindowEnabled, hWnd);
    }

    // =====================================================================
    // IsIconic
    // =====================================================================
    private static @Nullable MethodHandle isIconic;
    public static int IsIconic(MemorySegment hWnd) {
        if (isIconic == null) {
            isIconic = linker.downcallHandle(
                    user32.find("IsIconic").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(isIconic, hWnd);
    }

    // =====================================================================
    // IsZoomed
    // =====================================================================
    private static @Nullable MethodHandle isZoomed;
    public static int IsZoomed(MemorySegment hWnd) {
        if (isZoomed == null) {
            isZoomed = linker.downcallHandle(
                    user32.find("IsZoomed").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(isZoomed, hWnd);
    }

    // =====================================================================
    // BringWindowToTop
    // =====================================================================
    private static @Nullable MethodHandle bringWindowToTop;
    public static int BringWindowToTop(MemorySegment hWnd) {
        if (bringWindowToTop == null) {
            bringWindowToTop = linker.downcallHandle(
                    user32.find("BringWindowToTop").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(bringWindowToTop, hWnd);
    }

    // =====================================================================
    // SetWindowLongW
    // =====================================================================
    private static @Nullable MethodHandle setWindowLongW;
    public static int SetWindowLongW(MemorySegment hWnd, int nIndex, int dwNewLong) {
        if (setWindowLongW == null) {
            setWindowLongW = linker.downcallHandle(
                    user32.find("SetWindowLongW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setWindowLongW, hWnd, nIndex, dwNewLong);
    }

    // =====================================================================
    // GetWindowLongW
    // =====================================================================
    private static @Nullable MethodHandle getWindowLongW;
    public static int GetWindowLongW(MemorySegment hWnd, int nIndex) {
        if (getWindowLongW == null) {
            getWindowLongW = linker.downcallHandle(
                    user32.find("GetWindowLongW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getWindowLongW, hWnd, nIndex);
    }

    // =====================================================================
    // SetWindowLongPtrW
    // =====================================================================
    private static @Nullable MethodHandle setWindowLongPtrW;
    public static long SetWindowLongPtrW(MemorySegment hWnd, int nIndex, long dwNewLong) {
        if (setWindowLongPtrW == null) {
            setWindowLongPtrW = linker.downcallHandle(
                    user32.find("SetWindowLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(setWindowLongPtrW, hWnd, nIndex, dwNewLong);
    }

    // =====================================================================
    // GetWindowLongPtrW
    // =====================================================================
    private static @Nullable MethodHandle getWindowLongPtrW;
    public static long GetWindowLongPtrW(MemorySegment hWnd, int nIndex) {
        if (getWindowLongPtrW == null) {
            getWindowLongPtrW = linker.downcallHandle(
                    user32.find("GetWindowLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getWindowLongPtrW, hWnd, nIndex);
    }

    // =====================================================================
    // GetParent
    // =====================================================================
    private static @Nullable MethodHandle getParent;
    public static MemorySegment GetParent(MemorySegment hWnd) {
        if (getParent == null) {
            getParent = linker.downcallHandle(
                    user32.find("GetParent").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getParent, hWnd);
    }

    // =====================================================================
    // SetParent
    // =====================================================================
    private static @Nullable MethodHandle setParent;
    public static MemorySegment SetParent(MemorySegment hWndChild, MemorySegment hWndNewParent) {
        if (setParent == null) {
            setParent = linker.downcallHandle(
                    user32.find("SetParent").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setParent, hWndChild, hWndNewParent);
    }

    // =====================================================================
    // GetWindow
    // =====================================================================
    private static @Nullable MethodHandle getWindow;
    public static MemorySegment GetWindow(MemorySegment hWnd, int uCmd) {
        if (getWindow == null) {
            getWindow = linker.downcallHandle(
                    user32.find("GetWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getWindow, hWnd, uCmd);
    }

    // =====================================================================
    // GetTopWindow
    // =====================================================================
    private static @Nullable MethodHandle getTopWindow;
    public static MemorySegment GetTopWindow(MemorySegment hWnd) {
        if (getTopWindow == null) {
            getTopWindow = linker.downcallHandle(
                    user32.find("GetTopWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getTopWindow, hWnd);
    }

    // =====================================================================
    // EnumWindows
    // =====================================================================
    private static @Nullable MethodHandle enumWindows;
    public static int EnumWindows(MemorySegment lpEnumFunc, long lParam) {
        if (enumWindows == null) {
            enumWindows = linker.downcallHandle(
                    user32.find("EnumWindows").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(enumWindows, lpEnumFunc, lParam);
    }

    // =====================================================================
    // EnumChildWindows
    // =====================================================================
    private static @Nullable MethodHandle enumChildWindows;
    public static int EnumChildWindows(MemorySegment hWndParent, MemorySegment lpEnumFunc, long lParam) {
        if (enumChildWindows == null) {
            enumChildWindows = linker.downcallHandle(
                    user32.find("EnumChildWindows").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(enumChildWindows, hWndParent, lpEnumFunc, lParam);
    }

    // =====================================================================
    // EnumThreadWindows
    // =====================================================================
    private static @Nullable MethodHandle enumThreadWindows;
    public static int EnumThreadWindows(int dwThreadId, MemorySegment lpfn, long lParam) {
        if (enumThreadWindows == null) {
            enumThreadWindows = linker.downcallHandle(
                    user32.find("EnumThreadWindows").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(enumThreadWindows, dwThreadId, lpfn, lParam);
    }

    // =====================================================================
    // GetWindowThreadProcessId
    // =====================================================================
    private static @Nullable MethodHandle getWindowThreadProcessId;
    public static int GetWindowThreadProcessId(MemorySegment hWnd, MemorySegment lpdwProcessId) {
        if (getWindowThreadProcessId == null) {
            getWindowThreadProcessId = linker.downcallHandle(
                    user32.find("GetWindowThreadProcessId").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getWindowThreadProcessId, hWnd, lpdwProcessId);
    }

    // =====================================================================
    // GetMessageW
    // =====================================================================
    private static @Nullable MethodHandle getMessageW;
    public static int GetMessageW(MemorySegment lpMsg, MemorySegment hWnd, int wMsgFilterMin, int wMsgFilterMax) {
        if (getMessageW == null) {
            getMessageW = linker.downcallHandle(
                    user32.find("GetMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getMessageW, lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax);
    }

    // =====================================================================
    // PeekMessageW
    // =====================================================================
    private static @Nullable MethodHandle peekMessageW;
    public static int PeekMessageW(MemorySegment lpMsg, MemorySegment hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg) {
        if (peekMessageW == null) {
            peekMessageW = linker.downcallHandle(
                    user32.find("PeekMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(peekMessageW, lpMsg, hWnd, wMsgFilterMin, wMsgFilterMax, wRemoveMsg);
    }

    // =====================================================================
    // TranslateMessage
    // =====================================================================
    private static @Nullable MethodHandle translateMessage;
    public static int TranslateMessage(MemorySegment lpMsg) {
        if (translateMessage == null) {
            translateMessage = linker.downcallHandle(
                    user32.find("TranslateMessage").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(translateMessage, lpMsg);
    }

    // =====================================================================
    // DispatchMessageW
    // =====================================================================
    private static @Nullable MethodHandle dispatchMessageW;
    public static long DispatchMessageW(MemorySegment lpMsg) {
        if (dispatchMessageW == null) {
            dispatchMessageW = linker.downcallHandle(
                    user32.find("DispatchMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(dispatchMessageW, lpMsg);
    }

    // =====================================================================
    // SendMessageW
    // =====================================================================
    private static @Nullable MethodHandle sendMessageW;
    public static long SendMessageW(MemorySegment hWnd, int Msg, long wParam, long lParam) {
        if (sendMessageW == null) {
            sendMessageW = linker.downcallHandle(
                    user32.find("SendMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(sendMessageW, hWnd, Msg, wParam, lParam);
    }

    // =====================================================================
    // PostMessageW
    // =====================================================================
    private static @Nullable MethodHandle postMessageW;
    public static int PostMessageW(MemorySegment hWnd, int Msg, long wParam, long lParam) {
        if (postMessageW == null) {
            postMessageW = linker.downcallHandle(
                    user32.find("PostMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(postMessageW, hWnd, Msg, wParam, lParam);
    }

    // =====================================================================
    // PostThreadMessageW
    // =====================================================================
    private static @Nullable MethodHandle postThreadMessageW;
    public static int PostThreadMessageW(int idThread, int Msg, long wParam, long lParam) {
        if (postThreadMessageW == null) {
            postThreadMessageW = linker.downcallHandle(
                    user32.find("PostThreadMessageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(postThreadMessageW, idThread, Msg, wParam, lParam);
    }

    // =====================================================================
    // PostQuitMessage
    // =====================================================================
    private static @Nullable MethodHandle postQuitMessage;
    public static void PostQuitMessage(int nExitCode) {
        if (postQuitMessage == null) {
            postQuitMessage = linker.downcallHandle(
                    user32.find("PostQuitMessage").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT)
            );
        }

        invoke(postQuitMessage, nExitCode);
    }

    // =====================================================================
    // WaitMessage
    // =====================================================================
    private static @Nullable MethodHandle waitMessage;
    public static int WaitMessage() {
        if (waitMessage == null) {
            waitMessage = linker.downcallHandle(
                    user32.find("WaitMessage").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(waitMessage);
    }

    // =====================================================================
    // DefWindowProcW
    // =====================================================================
    private static @Nullable MethodHandle defWindowProcW;
    public static long DefWindowProcW(MemorySegment hWnd, int Msg, long wParam, long lParam) {
        if (defWindowProcW == null) {
            defWindowProcW = linker.downcallHandle(
                    user32.find("DefWindowProcW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(defWindowProcW, hWnd, Msg, wParam, lParam);
    }

    // =====================================================================
    // RegisterClassExW
    // =====================================================================
    private static @Nullable MethodHandle registerClassExW;
    public static short RegisterClassExW(MemorySegment lpwcx) {
        if (registerClassExW == null) {
            registerClassExW = linker.downcallHandle(
                    user32.find("RegisterClassExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_SHORT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(registerClassExW, lpwcx);
    }

    // =====================================================================
    // UnregisterClassW
    // =====================================================================
    private static @Nullable MethodHandle unregisterClassW;
    public static int UnregisterClassW(MemorySegment lpClassName, MemorySegment hInstance) {
        if (unregisterClassW == null) {
            unregisterClassW = linker.downcallHandle(
                    user32.find("UnregisterClassW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(unregisterClassW, lpClassName, hInstance);
    }

    // =====================================================================
    // GetClassInfoExW
    // =====================================================================
    private static @Nullable MethodHandle getClassInfoExW;
    public static int GetClassInfoExW(MemorySegment hInstance, MemorySegment lpszClass, MemorySegment lpwcx) {
        if (getClassInfoExW == null) {
            getClassInfoExW = linker.downcallHandle(
                    user32.find("GetClassInfoExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getClassInfoExW, hInstance, lpszClass, lpwcx);
    }

    // =====================================================================
    // GetKeyState
    // =====================================================================
    private static @Nullable MethodHandle getKeyState;
    public static short GetKeyState(int nVirtKey) {
        if (getKeyState == null) {
            getKeyState = linker.downcallHandle(
                    user32.find("GetKeyState").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_SHORT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getKeyState, nVirtKey);
    }

    // =====================================================================
    // GetAsyncKeyState
    // =====================================================================
    private static @Nullable MethodHandle getAsyncKeyState;
    public static short GetAsyncKeyState(int vKey) {
        if (getAsyncKeyState == null) {
            getAsyncKeyState = linker.downcallHandle(
                    user32.find("GetAsyncKeyState").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_SHORT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getAsyncKeyState, vKey);
    }

    // =====================================================================
    // GetKeyboardState
    // =====================================================================
    private static @Nullable MethodHandle getKeyboardState;
    public static int GetKeyboardState(MemorySegment lpKeyState) {
        if (getKeyboardState == null) {
            getKeyboardState = linker.downcallHandle(
                    user32.find("GetKeyboardState").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getKeyboardState, lpKeyState);
    }

    // =====================================================================
    // SetKeyboardState
    // =====================================================================
    private static @Nullable MethodHandle setKeyboardState;
    public static int SetKeyboardState(MemorySegment lpKeyState) {
        if (setKeyboardState == null) {
            setKeyboardState = linker.downcallHandle(
                    user32.find("SetKeyboardState").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setKeyboardState, lpKeyState);
    }

    // =====================================================================
    // GetCursorPos
    // =====================================================================
    private static @Nullable MethodHandle getCursorPos;
    public static int GetCursorPos(MemorySegment lpPoint) {
        if (getCursorPos == null) {
            getCursorPos = linker.downcallHandle(
                    user32.find("GetCursorPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getCursorPos, lpPoint);
    }

    // =====================================================================
    // SetCursorPos
    // =====================================================================
    private static @Nullable MethodHandle setCursorPos;
    public static int SetCursorPos(int X, int Y) {
        if (setCursorPos == null) {
            setCursorPos = linker.downcallHandle(
                    user32.find("SetCursorPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setCursorPos, X, Y);
    }

    // =====================================================================
    // ClipCursor
    // =====================================================================
    private static @Nullable MethodHandle clipCursor;
    public static int ClipCursor(MemorySegment lpRect) {
        if (clipCursor == null) {
            clipCursor = linker.downcallHandle(
                    user32.find("ClipCursor").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(clipCursor, lpRect);
    }

    // =====================================================================
    // ShowCursor
    // =====================================================================
    private static @Nullable MethodHandle showCursor;
    public static int ShowCursor(int bShow) {
        if (showCursor == null) {
            showCursor = linker.downcallHandle(
                    user32.find("ShowCursor").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(showCursor, bShow);
    }

    // =====================================================================
    // SetCursor
    // =====================================================================
    private static @Nullable MethodHandle setCursor;
    public static MemorySegment SetCursor(MemorySegment hCursor) {
        if (setCursor == null) {
            setCursor = linker.downcallHandle(
                    user32.find("SetCursor").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setCursor, hCursor);
    }

    // =====================================================================
    // GetCursor
    // =====================================================================
    private static @Nullable MethodHandle getCursor;
    public static MemorySegment GetCursor() {
        if (getCursor == null) {
            getCursor = linker.downcallHandle(
                    user32.find("GetCursor").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getCursor);
    }

    // =====================================================================
    // LoadCursorW
    // =====================================================================
    private static @Nullable MethodHandle loadCursorW;
    public static MemorySegment LoadCursorW(MemorySegment hInstance, MemorySegment lpCursorName) {
        if (loadCursorW == null) {
            loadCursorW = linker.downcallHandle(
                    user32.find("LoadCursorW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(loadCursorW, hInstance, lpCursorName);
    }

    // =====================================================================
    // DestroyCursor
    // =====================================================================
    private static @Nullable MethodHandle destroyCursor;
    public static int DestroyCursor(MemorySegment hCursor) {
        if (destroyCursor == null) {
            destroyCursor = linker.downcallHandle(
                    user32.find("DestroyCursor").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(destroyCursor, hCursor);
    }

    // =====================================================================
    // LoadIconW
    // =====================================================================
    private static @Nullable MethodHandle loadIconW;
    public static MemorySegment LoadIconW(MemorySegment hInstance, MemorySegment lpIconName) {
        if (loadIconW == null) {
            loadIconW = linker.downcallHandle(
                    user32.find("LoadIconW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(loadIconW, hInstance, lpIconName);
    }

    // =====================================================================
    // DestroyIcon
    // =====================================================================
    private static @Nullable MethodHandle destroyIcon;
    public static int DestroyIcon(MemorySegment hIcon) {
        if (destroyIcon == null) {
            destroyIcon = linker.downcallHandle(
                    user32.find("DestroyIcon").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(destroyIcon, hIcon);
    }

    // =====================================================================
    // LoadImageW
    // =====================================================================
    private static @Nullable MethodHandle loadImageW;
    public static MemorySegment LoadImageW(MemorySegment hInst, MemorySegment name, int type, int cx, int cy, int fuLoad) {
        if (loadImageW == null) {
            loadImageW = linker.downcallHandle(
                    user32.find("LoadImageW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(loadImageW, hInst, name, type, cx, cy, fuLoad);
    }

    // =====================================================================
    // OpenClipboard
    // =====================================================================
    private static @Nullable MethodHandle openClipboard;
    public static int OpenClipboard(MemorySegment hWndNewOwner) {
        if (openClipboard == null) {
            openClipboard = linker.downcallHandle(
                    user32.find("OpenClipboard").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(openClipboard, hWndNewOwner);
    }

    // =====================================================================
    // CloseClipboard
    // =====================================================================
    private static @Nullable MethodHandle closeClipboard;
    public static int CloseClipboard() {
        if (closeClipboard == null) {
            closeClipboard = linker.downcallHandle(
                    user32.find("CloseClipboard").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(closeClipboard);
    }

    // =====================================================================
    // EmptyClipboard
    // =====================================================================
    private static @Nullable MethodHandle emptyClipboard;
    public static int EmptyClipboard() {
        if (emptyClipboard == null) {
            emptyClipboard = linker.downcallHandle(
                    user32.find("EmptyClipboard").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(emptyClipboard);
    }

    // =====================================================================
    // GetClipboardData
    // =====================================================================
    private static @Nullable MethodHandle getClipboardData;
    public static MemorySegment GetClipboardData(int uFormat) {
        if (getClipboardData == null) {
            getClipboardData = linker.downcallHandle(
                    user32.find("GetClipboardData").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getClipboardData, uFormat);
    }

    // =====================================================================
    // SetClipboardData
    // =====================================================================
    private static @Nullable MethodHandle setClipboardData;
    public static MemorySegment SetClipboardData(int uFormat, MemorySegment hMem) {
        if (setClipboardData == null) {
            setClipboardData = linker.downcallHandle(
                    user32.find("SetClipboardData").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setClipboardData, uFormat, hMem);
    }

    // =====================================================================
    // IsClipboardFormatAvailable
    // =====================================================================
    private static @Nullable MethodHandle isClipboardFormatAvailable;
    public static int IsClipboardFormatAvailable(int format) {
        if (isClipboardFormatAvailable == null) {
            isClipboardFormatAvailable = linker.downcallHandle(
                    user32.find("IsClipboardFormatAvailable").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(isClipboardFormatAvailable, format);
    }

    // =====================================================================
    // RegisterClipboardFormatW
    // =====================================================================
    private static @Nullable MethodHandle registerClipboardFormatW;
    public static int RegisterClipboardFormatW(MemorySegment lpszFormat) {
        if (registerClipboardFormatW == null) {
            registerClipboardFormatW = linker.downcallHandle(
                    user32.find("RegisterClipboardFormatW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(registerClipboardFormatW, lpszFormat);
    }

    // =====================================================================
    // EnumClipboardFormats
    // =====================================================================
    private static @Nullable MethodHandle enumClipboardFormats;
    public static int EnumClipboardFormats(int format) {
        if (enumClipboardFormats == null) {
            enumClipboardFormats = linker.downcallHandle(
                    user32.find("EnumClipboardFormats").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(enumClipboardFormats, format);
    }

    // =====================================================================
    // GetClipboardOwner
    // =====================================================================
    private static @Nullable MethodHandle getClipboardOwner;
    public static MemorySegment GetClipboardOwner() {
        if (getClipboardOwner == null) {
            getClipboardOwner = linker.downcallHandle(
                    user32.find("GetClipboardOwner").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getClipboardOwner);
    }

    // =====================================================================
    // GetDC
    // =====================================================================
    private static @Nullable MethodHandle getDC;
    public static MemorySegment GetDC(MemorySegment hWnd) {
        if (getDC == null) {
            getDC = linker.downcallHandle(
                    user32.find("GetDC").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getDC, hWnd);
    }

    // =====================================================================
    // GetWindowDC
    // =====================================================================
    private static @Nullable MethodHandle getWindowDC;
    public static MemorySegment GetWindowDC(MemorySegment hWnd) {
        if (getWindowDC == null) {
            getWindowDC = linker.downcallHandle(
                    user32.find("GetWindowDC").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getWindowDC, hWnd);
    }

    // =====================================================================
    // ReleaseDC
    // =====================================================================
    private static @Nullable MethodHandle releaseDC;
    public static int ReleaseDC(MemorySegment hWnd, MemorySegment hDC) {
        if (releaseDC == null) {
            releaseDC = linker.downcallHandle(
                    user32.find("ReleaseDC").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(releaseDC, hWnd, hDC);
    }

    // =====================================================================
    // BeginPaint
    // =====================================================================
    private static @Nullable MethodHandle beginPaint;
    public static MemorySegment BeginPaint(MemorySegment hWnd, MemorySegment lpPaint) {
        if (beginPaint == null) {
            beginPaint = linker.downcallHandle(
                    user32.find("BeginPaint").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(beginPaint, hWnd, lpPaint);
    }

    // =====================================================================
    // EndPaint
    // =====================================================================
    private static @Nullable MethodHandle endPaint;
    public static int EndPaint(MemorySegment hWnd, MemorySegment lpPaint) {
        if (endPaint == null) {
            endPaint = linker.downcallHandle(
                    user32.find("EndPaint").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(endPaint, hWnd, lpPaint);
    }

    // =====================================================================
    // InvalidateRect
    // =====================================================================
    private static @Nullable MethodHandle invalidateRect;
    public static int InvalidateRect(MemorySegment hWnd, MemorySegment lpRect, int bErase) {
        if (invalidateRect == null) {
            invalidateRect = linker.downcallHandle(
                    user32.find("InvalidateRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(invalidateRect, hWnd, lpRect, bErase);
    }

    // =====================================================================
    // ValidateRect
    // =====================================================================
    private static @Nullable MethodHandle validateRect;
    public static int ValidateRect(MemorySegment hWnd, MemorySegment lpRect) {
        if (validateRect == null) {
            validateRect = linker.downcallHandle(
                    user32.find("ValidateRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(validateRect, hWnd, lpRect);
    }

    // =====================================================================
    // RedrawWindow
    // =====================================================================
    private static @Nullable MethodHandle redrawWindow;
    public static int RedrawWindow(MemorySegment hWnd, MemorySegment lprcUpdate, MemorySegment hrgnUpdate, int flags) {
        if (redrawWindow == null) {
            redrawWindow = linker.downcallHandle(
                    user32.find("RedrawWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(redrawWindow, hWnd, lprcUpdate, hrgnUpdate, flags);
    }

    // =====================================================================
    // FillRect
    // =====================================================================
    private static @Nullable MethodHandle fillRect;
    public static int FillRect(MemorySegment hDC, MemorySegment lprc, MemorySegment hbr) {
        if (fillRect == null) {
            fillRect = linker.downcallHandle(
                    user32.find("FillRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(fillRect, hDC, lprc, hbr);
    }

    // =====================================================================
    // FrameRect
    // =====================================================================
    private static @Nullable MethodHandle frameRect;
    public static int FrameRect(MemorySegment hDC, MemorySegment lprc, MemorySegment hbr) {
        if (frameRect == null) {
            frameRect = linker.downcallHandle(
                    user32.find("FrameRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(frameRect, hDC, lprc, hbr);
    }

    // =====================================================================
    // InvertRect
    // =====================================================================
    private static @Nullable MethodHandle invertRect;
    public static int InvertRect(MemorySegment hDC, MemorySegment lprc) {
        if (invertRect == null) {
            invertRect = linker.downcallHandle(
                    user32.find("InvertRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(invertRect, hDC, lprc);
    }

    // =====================================================================
    // DrawTextW
    // =====================================================================
    private static @Nullable MethodHandle drawTextW;
    public static int DrawTextW(MemorySegment hdc, MemorySegment lpchText, int cchText, MemorySegment lprc, int format) {
        if (drawTextW == null) {
            drawTextW = linker.downcallHandle(
                    user32.find("DrawTextW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(drawTextW, hdc, lpchText, cchText, lprc, format);
    }

    // =====================================================================
    // CreateMenu
    // =====================================================================
    private static @Nullable MethodHandle createMenu;
    public static MemorySegment CreateMenu() {
        if (createMenu == null) {
            createMenu = linker.downcallHandle(
                    user32.find("CreateMenu").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(createMenu);
    }

    // =====================================================================
    // CreatePopupMenu
    // =====================================================================
    private static @Nullable MethodHandle createPopupMenu;
    public static MemorySegment CreatePopupMenu() {
        if (createPopupMenu == null) {
            createPopupMenu = linker.downcallHandle(
                    user32.find("CreatePopupMenu").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(createPopupMenu);
    }

    // =====================================================================
    // DestroyMenu
    // =====================================================================
    private static @Nullable MethodHandle destroyMenu;
    public static int DestroyMenu(MemorySegment hMenu) {
        if (destroyMenu == null) {
            destroyMenu = linker.downcallHandle(
                    user32.find("DestroyMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(destroyMenu, hMenu);
    }

    // =====================================================================
    // AppendMenuW
    // =====================================================================
    private static @Nullable MethodHandle appendMenuW;
    public static int AppendMenuW(MemorySegment hMenu, int uFlags, long uIDNewItem, MemorySegment lpNewItem) {
        if (appendMenuW == null) {
            appendMenuW = linker.downcallHandle(
                    user32.find("AppendMenuW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(appendMenuW, hMenu, uFlags, uIDNewItem, lpNewItem);
    }

    // =====================================================================
    // InsertMenuW
    // =====================================================================
    private static @Nullable MethodHandle insertMenuW;
    public static int InsertMenuW(MemorySegment hMenu, int uPosition, int uFlags, long uIDNewItem, MemorySegment lpNewItem) {
        if (insertMenuW == null) {
            insertMenuW = linker.downcallHandle(
                    user32.find("InsertMenuW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(insertMenuW, hMenu, uPosition, uFlags, uIDNewItem, lpNewItem);
    }

    // =====================================================================
    // InsertMenuItemW
    // =====================================================================
    private static @Nullable MethodHandle insertMenuItemW;
    public static int InsertMenuItemW(MemorySegment hmenu, int item, int fByPosition, MemorySegment lpmi) {
        if (insertMenuItemW == null) {
            insertMenuItemW = linker.downcallHandle(
                    user32.find("InsertMenuItemW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(insertMenuItemW, hmenu, item, fByPosition, lpmi);
    }

    // =====================================================================
    // DeleteMenu
    // =====================================================================
    private static @Nullable MethodHandle deleteMenu;
    public static int DeleteMenu(MemorySegment hMenu, int uPosition, int uFlags) {
        if (deleteMenu == null) {
            deleteMenu = linker.downcallHandle(
                    user32.find("DeleteMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(deleteMenu, hMenu, uPosition, uFlags);
    }

    // =====================================================================
    // RemoveMenu
    // =====================================================================
    private static @Nullable MethodHandle removeMenu;
    public static int RemoveMenu(MemorySegment hMenu, int uPosition, int uFlags) {
        if (removeMenu == null) {
            removeMenu = linker.downcallHandle(
                    user32.find("RemoveMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(removeMenu, hMenu, uPosition, uFlags);
    }

    // =====================================================================
    // TrackPopupMenu
    // =====================================================================
    private static @Nullable MethodHandle trackPopupMenu;
    public static int TrackPopupMenu(MemorySegment hMenu, int uFlags, int x, int y, int nReserved, MemorySegment hWnd, MemorySegment prcRect) {
        if (trackPopupMenu == null) {
            trackPopupMenu = linker.downcallHandle(
                    user32.find("TrackPopupMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(trackPopupMenu, hMenu, uFlags, x, y, nReserved, hWnd, prcRect);
    }

    // =====================================================================
    // TrackPopupMenuEx
    // =====================================================================
    private static @Nullable MethodHandle trackPopupMenuEx;
    public static int TrackPopupMenuEx(MemorySegment hMenu, int uFlags, int x, int y, MemorySegment hwnd, MemorySegment lptpm) {
        if (trackPopupMenuEx == null) {
            trackPopupMenuEx = linker.downcallHandle(
                    user32.find("TrackPopupMenuEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(trackPopupMenuEx, hMenu, uFlags, x, y, hwnd, lptpm);
    }

    // =====================================================================
    // GetMenu
    // =====================================================================
    private static @Nullable MethodHandle getMenu;
    public static MemorySegment GetMenu(MemorySegment hWnd) {
        if (getMenu == null) {
            getMenu = linker.downcallHandle(
                    user32.find("GetMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getMenu, hWnd);
    }

    // =====================================================================
    // SetMenu
    // =====================================================================
    private static @Nullable MethodHandle setMenu;
    public static int SetMenu(MemorySegment hWnd, MemorySegment hMenu) {
        if (setMenu == null) {
            setMenu = linker.downcallHandle(
                    user32.find("SetMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setMenu, hWnd, hMenu);
    }

    // =====================================================================
    // GetSubMenu
    // =====================================================================
    private static @Nullable MethodHandle getSubMenu;
    public static MemorySegment GetSubMenu(MemorySegment hMenu, int nPos) {
        if (getSubMenu == null) {
            getSubMenu = linker.downcallHandle(
                    user32.find("GetSubMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getSubMenu, hMenu, nPos);
    }

    // =====================================================================
    // GetSystemMenu
    // =====================================================================
    private static @Nullable MethodHandle getSystemMenu;
    public static MemorySegment GetSystemMenu(MemorySegment hWnd, int bRevert) {
        if (getSystemMenu == null) {
            getSystemMenu = linker.downcallHandle(
                    user32.find("GetSystemMenu").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getSystemMenu, hWnd, bRevert);
    }

    // =====================================================================
    // SetWindowsHookExW
    // =====================================================================
    private static @Nullable MethodHandle setWindowsHookExW;
    public static MemorySegment SetWindowsHookExW(int idHook, MemorySegment lpfn, MemorySegment hmod, int dwThreadId) {
        if (setWindowsHookExW == null) {
            setWindowsHookExW = linker.downcallHandle(
                    user32.find("SetWindowsHookExW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setWindowsHookExW, idHook, lpfn, hmod, dwThreadId);
    }

    // =====================================================================
    // UnhookWindowsHookEx
    // =====================================================================
    private static @Nullable MethodHandle unhookWindowsHookEx;
    public static int UnhookWindowsHookEx(MemorySegment hhk) {
        if (unhookWindowsHookEx == null) {
            unhookWindowsHookEx = linker.downcallHandle(
                    user32.find("UnhookWindowsHookEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(unhookWindowsHookEx, hhk);
    }

    // =====================================================================
    // CallNextHookEx
    // =====================================================================
    private static @Nullable MethodHandle callNextHookEx;
    public static long CallNextHookEx(MemorySegment hhk, int nCode, long wParam, long lParam) {
        if (callNextHookEx == null) {
            callNextHookEx = linker.downcallHandle(
                    user32.find("CallNextHookEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(callNextHookEx, hhk, nCode, wParam, lParam);
    }

    // =====================================================================
    // SetTimer
    // =====================================================================
    private static @Nullable MethodHandle setTimer;
    public static long SetTimer(MemorySegment hWnd, long nIDEvent, int uElapse, MemorySegment lpTimerFunc) {
        if (setTimer == null) {
            setTimer = linker.downcallHandle(
                    user32.find("SetTimer").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setTimer, hWnd, nIDEvent, uElapse, lpTimerFunc);
    }

    // =====================================================================
    // KillTimer
    // =====================================================================
    private static @Nullable MethodHandle killTimer;
    public static int KillTimer(MemorySegment hWnd, long uIDEvent) {
        if (killTimer == null) {
            killTimer = linker.downcallHandle(
                    user32.find("KillTimer").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(killTimer, hWnd, uIDEvent);
    }

    // =====================================================================
    // CreateCaret
    // =====================================================================
    private static @Nullable MethodHandle createCaret;
    public static int CreateCaret(MemorySegment hWnd, MemorySegment hBitmap, int nWidth, int nHeight) {
        if (createCaret == null) {
            createCaret = linker.downcallHandle(
                    user32.find("CreateCaret").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(createCaret, hWnd, hBitmap, nWidth, nHeight);
    }

    // =====================================================================
    // DestroyCaret
    // =====================================================================
    private static @Nullable MethodHandle destroyCaret;
    public static int DestroyCaret() {
        if (destroyCaret == null) {
            destroyCaret = linker.downcallHandle(
                    user32.find("DestroyCaret").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(destroyCaret);
    }

    // =====================================================================
    // ShowCaret
    // =====================================================================
    private static @Nullable MethodHandle showCaret;
    public static int ShowCaret(MemorySegment hWnd) {
        if (showCaret == null) {
            showCaret = linker.downcallHandle(
                    user32.find("ShowCaret").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(showCaret, hWnd);
    }

    // =====================================================================
    // HideCaret
    // =====================================================================
    private static @Nullable MethodHandle hideCaret;
    public static int HideCaret(MemorySegment hWnd) {
        if (hideCaret == null) {
            hideCaret = linker.downcallHandle(
                    user32.find("HideCaret").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(hideCaret, hWnd);
    }

    // =====================================================================
    // SetCaretPos
    // =====================================================================
    private static @Nullable MethodHandle setCaretPos;
    public static int SetCaretPos(int X, int Y) {
        if (setCaretPos == null) {
            setCaretPos = linker.downcallHandle(
                    user32.find("SetCaretPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setCaretPos, X, Y);
    }

    // =====================================================================
    // GetCaretPos
    // =====================================================================
    private static @Nullable MethodHandle getCaretPos;
    public static int GetCaretPos(MemorySegment lpPoint) {
        if (getCaretPos == null) {
            getCaretPos = linker.downcallHandle(
                    user32.find("GetCaretPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getCaretPos, lpPoint);
    }

    // =====================================================================
    // SetCaretBlinkTime
    // =====================================================================
    private static @Nullable MethodHandle setCaretBlinkTime;
    public static int SetCaretBlinkTime(int uMSeconds) {
        if (setCaretBlinkTime == null) {
            setCaretBlinkTime = linker.downcallHandle(
                    user32.find("SetCaretBlinkTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setCaretBlinkTime, uMSeconds);
    }

    // =====================================================================
    // GetCaretBlinkTime
    // =====================================================================
    private static @Nullable MethodHandle getCaretBlinkTime;
    public static int GetCaretBlinkTime() {
        if (getCaretBlinkTime == null) {
            getCaretBlinkTime = linker.downcallHandle(
                    user32.find("GetCaretBlinkTime").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getCaretBlinkTime);
    }

    // =====================================================================
    // GetSystemMetrics
    // =====================================================================
    private static @Nullable MethodHandle getSystemMetrics;
    public static int GetSystemMetrics(int nIndex) {
        if (getSystemMetrics == null) {
            getSystemMetrics = linker.downcallHandle(
                    user32.find("GetSystemMetrics").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getSystemMetrics, nIndex);
    }

    // =====================================================================
    // SystemParametersInfoW
    // =====================================================================
    private static @Nullable MethodHandle systemParametersInfoW;
    public static int SystemParametersInfoW(int uiAction, int uiParam, MemorySegment pvParam, int fWinIni) {
        if (systemParametersInfoW == null) {
            systemParametersInfoW = linker.downcallHandle(
                    user32.find("SystemParametersInfoW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(systemParametersInfoW, uiAction, uiParam, pvParam, fWinIni);
    }

    // =====================================================================
    // GetDlgItem
    // =====================================================================
    private static @Nullable MethodHandle getDlgItem;
    public static MemorySegment GetDlgItem(MemorySegment hDlg, int nIDDlgItem) {
        if (getDlgItem == null) {
            getDlgItem = linker.downcallHandle(
                    user32.find("GetDlgItem").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getDlgItem, hDlg, nIDDlgItem);
    }

    // =====================================================================
    // SetDlgItemTextW
    // =====================================================================
    private static @Nullable MethodHandle setDlgItemTextW;
    public static int SetDlgItemTextW(MemorySegment hDlg, int nIDDlgItem, MemorySegment lpString) {
        if (setDlgItemTextW == null) {
            setDlgItemTextW = linker.downcallHandle(
                    user32.find("SetDlgItemTextW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setDlgItemTextW, hDlg, nIDDlgItem, lpString);
    }

    // =====================================================================
    // GetDlgItemTextW
    // =====================================================================
    private static @Nullable MethodHandle getDlgItemTextW;
    public static int GetDlgItemTextW(MemorySegment hDlg, int nIDDlgItem, MemorySegment lpString, int cchMax) {
        if (getDlgItemTextW == null) {
            getDlgItemTextW = linker.downcallHandle(
                    user32.find("GetDlgItemTextW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getDlgItemTextW, hDlg, nIDDlgItem, lpString, cchMax);
    }

    // =====================================================================
    // SetDlgItemInt
    // =====================================================================
    private static @Nullable MethodHandle setDlgItemInt;
    public static int SetDlgItemInt(MemorySegment hDlg, int nIDDlgItem, int uValue, int bSigned) {
        if (setDlgItemInt == null) {
            setDlgItemInt = linker.downcallHandle(
                    user32.find("SetDlgItemInt").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setDlgItemInt, hDlg, nIDDlgItem, uValue, bSigned);
    }

    // =====================================================================
    // GetDlgItemInt
    // =====================================================================
    private static @Nullable MethodHandle getDlgItemInt;
    public static int GetDlgItemInt(MemorySegment hDlg, int nIDDlgItem, MemorySegment lpTranslated, int bSigned) {
        if (getDlgItemInt == null) {
            getDlgItemInt = linker.downcallHandle(
                    user32.find("GetDlgItemInt").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getDlgItemInt, hDlg, nIDDlgItem, lpTranslated, bSigned);
    }

    // =====================================================================
    // EndDialog
    // =====================================================================
    private static @Nullable MethodHandle endDialog;
    public static int EndDialog(MemorySegment hDlg, long nResult) {
        if (endDialog == null) {
            endDialog = linker.downcallHandle(
                    user32.find("EndDialog").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(endDialog, hDlg, nResult);
    }

    // =====================================================================
    // ClientToScreen
    // =====================================================================
    private static @Nullable MethodHandle clientToScreen;
    public static int ClientToScreen(MemorySegment hWnd, MemorySegment lpPoint) {
        if (clientToScreen == null) {
            clientToScreen = linker.downcallHandle(
                    user32.find("ClientToScreen").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(clientToScreen, hWnd, lpPoint);
    }

    // =====================================================================
    // ScreenToClient
    // =====================================================================
    private static @Nullable MethodHandle screenToClient;
    public static int ScreenToClient(MemorySegment hWnd, MemorySegment lpPoint) {
        if (screenToClient == null) {
            screenToClient = linker.downcallHandle(
                    user32.find("ScreenToClient").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(screenToClient, hWnd, lpPoint);
    }

    // =====================================================================
    // MapWindowPoints
    // =====================================================================
    private static @Nullable MethodHandle mapWindowPoints;
    public static int MapWindowPoints(MemorySegment hWndFrom, MemorySegment hWndTo, MemorySegment lpPoints, int cPoints) {
        if (mapWindowPoints == null) {
            mapWindowPoints = linker.downcallHandle(
                    user32.find("MapWindowPoints").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(mapWindowPoints, hWndFrom, hWndTo, lpPoints, cPoints);
    }

    // =====================================================================
    // SetCapture
    // =====================================================================
    private static @Nullable MethodHandle setCapture;
    public static MemorySegment SetCapture(MemorySegment hWnd) {
        if (setCapture == null) {
            setCapture = linker.downcallHandle(
                    user32.find("SetCapture").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(setCapture, hWnd);
    }

    // =====================================================================
    // GetCapture
    // =====================================================================
    private static @Nullable MethodHandle getCapture;
    public static MemorySegment GetCapture() {
        if (getCapture == null) {
            getCapture = linker.downcallHandle(
                    user32.find("GetCapture").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS)
            );
        }

        return invoke(getCapture);
    }

    // =====================================================================
    // ReleaseCapture
    // =====================================================================
    private static @Nullable MethodHandle releaseCapture;
    public static int ReleaseCapture() {
        if (releaseCapture == null) {
            releaseCapture = linker.downcallHandle(
                    user32.find("ReleaseCapture").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(releaseCapture);
    }

    // =====================================================================
    // GetDoubleClickTime
    // =====================================================================
    private static @Nullable MethodHandle getDoubleClickTime;
    public static int GetDoubleClickTime() {
        if (getDoubleClickTime == null) {
            getDoubleClickTime = linker.downcallHandle(
                    user32.find("GetDoubleClickTime").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        }

        return invoke(getDoubleClickTime);
    }

    // =====================================================================
    // SetDoubleClickTime
    // =====================================================================
    private static @Nullable MethodHandle setDoubleClickTime;
    public static int SetDoubleClickTime(int uInterval) {
        if (setDoubleClickTime == null) {
            setDoubleClickTime = linker.downcallHandle(
                    user32.find("SetDoubleClickTime").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setDoubleClickTime, uInterval);
    }

    // =====================================================================
    // SendInput
    // =====================================================================
    private static @Nullable MethodHandle sendInput;
    public static int SendInput(int cInputs, MemorySegment pInputs, int cbSize) {
        if (sendInput == null) {
            sendInput = linker.downcallHandle(
                    user32.find("SendInput").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(sendInput, cInputs, pInputs, cbSize);
    }

    // =====================================================================
    // BlockInput
    // =====================================================================
    private static @Nullable MethodHandle blockInput;
    public static int BlockInput(int fBlockIt) {
        if (blockInput == null) {
            blockInput = linker.downcallHandle(
                    user32.find("BlockInput").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(blockInput, fBlockIt);
    }

    // =====================================================================
    // MonitorFromWindow
    // =====================================================================
    private static @Nullable MethodHandle monitorFromWindow;
    public static MemorySegment MonitorFromWindow(MemorySegment hwnd, int dwFlags) {
        if (monitorFromWindow == null) {
            monitorFromWindow = linker.downcallHandle(
                    user32.find("MonitorFromWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(monitorFromWindow, hwnd, dwFlags);
    }

    // =====================================================================
    // MonitorFromPoint
    // =====================================================================
    private static @Nullable MethodHandle monitorFromPoint;
    public static MemorySegment MonitorFromPoint(long pt, int dwFlags) {
        if (monitorFromPoint == null) {
            monitorFromPoint = linker.downcallHandle(
                    user32.find("MonitorFromPoint").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(monitorFromPoint, pt, dwFlags);
    }

    // =====================================================================
    // GetMonitorInfoW
    // =====================================================================
    private static @Nullable MethodHandle getMonitorInfoW;
    public static int GetMonitorInfoW(MemorySegment hMonitor, MemorySegment lpmi) {
        if (getMonitorInfoW == null) {
            getMonitorInfoW = linker.downcallHandle(
                    user32.find("GetMonitorInfoW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getMonitorInfoW, hMonitor, lpmi);
    }

    // =====================================================================
    // EnumDisplayMonitors
    // =====================================================================
    private static @Nullable MethodHandle enumDisplayMonitors;
    public static int EnumDisplayMonitors(MemorySegment hdc, MemorySegment lprcClip, MemorySegment lpfnEnum, long dwData) {
        if (enumDisplayMonitors == null) {
            enumDisplayMonitors = linker.downcallHandle(
                    user32.find("EnumDisplayMonitors").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(enumDisplayMonitors, hdc, lprcClip, lpfnEnum, dwData);
    }

    // =====================================================================
    // GetSysColor
    // =====================================================================
    private static @Nullable MethodHandle getSysColor;
    public static int GetSysColor(int nIndex) {
        if (getSysColor == null) {
            getSysColor = linker.downcallHandle(
                    user32.find("GetSysColor").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getSysColor, nIndex);
    }

    // =====================================================================
    // GetSysColorBrush
    // =====================================================================
    private static @Nullable MethodHandle getSysColorBrush;
    public static MemorySegment GetSysColorBrush(int nIndex) {
        if (getSysColorBrush == null) {
            getSysColorBrush = linker.downcallHandle(
                    user32.find("GetSysColorBrush").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getSysColorBrush, nIndex);
    }

    // =====================================================================
    // AdjustWindowRect
    // =====================================================================
    private static @Nullable MethodHandle adjustWindowRect;
    public static int AdjustWindowRect(MemorySegment lpRect, int dwStyle, int bMenu) {
        if (adjustWindowRect == null) {
            adjustWindowRect = linker.downcallHandle(
                    user32.find("AdjustWindowRect").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(adjustWindowRect, lpRect, dwStyle, bMenu);
    }

    // =====================================================================
    // AdjustWindowRectEx
    // =====================================================================
    private static @Nullable MethodHandle adjustWindowRectEx;
    public static int AdjustWindowRectEx(MemorySegment lpRect, int dwStyle, int bMenu, int dwExStyle) {
        if (adjustWindowRectEx == null) {
            adjustWindowRectEx = linker.downcallHandle(
                    user32.find("AdjustWindowRectEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(adjustWindowRectEx, lpRect, dwStyle, bMenu, dwExStyle);
    }

    // =====================================================================
    // SetLayeredWindowAttributes
    // =====================================================================
    private static @Nullable MethodHandle setLayeredWindowAttributes;
    public static int SetLayeredWindowAttributes(MemorySegment hwnd, int crKey, byte bAlpha, int dwFlags) {
        if (setLayeredWindowAttributes == null) {
            setLayeredWindowAttributes = linker.downcallHandle(
                    user32.find("SetLayeredWindowAttributes").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_BYTE,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setLayeredWindowAttributes, hwnd, crKey, bAlpha, dwFlags);
    }

    // =====================================================================
    // GetLayeredWindowAttributes
    // =====================================================================
    private static @Nullable MethodHandle getLayeredWindowAttributes;
    public static int GetLayeredWindowAttributes(MemorySegment hwnd, MemorySegment pcrKey, MemorySegment pbAlpha, MemorySegment pdwFlags) {
        if (getLayeredWindowAttributes == null) {
            getLayeredWindowAttributes = linker.downcallHandle(
                    user32.find("GetLayeredWindowAttributes").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getLayeredWindowAttributes, hwnd, pcrKey, pbAlpha, pdwFlags);
    }

    // =====================================================================
    // FlashWindow
    // =====================================================================
    private static @Nullable MethodHandle flashWindow;
    public static int FlashWindow(MemorySegment hWnd, int bInvert) {
        if (flashWindow == null) {
            flashWindow = linker.downcallHandle(
                    user32.find("FlashWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(flashWindow, hWnd, bInvert);
    }

    // =====================================================================
    // FlashWindowEx
    // =====================================================================
    private static @Nullable MethodHandle flashWindowEx;
    public static int FlashWindowEx(MemorySegment pfwi) {
        if (flashWindowEx == null) {
            flashWindowEx = linker.downcallHandle(
                    user32.find("FlashWindowEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(flashWindowEx, pfwi);
    }

    // =====================================================================
    // GetClassNameW
    // =====================================================================
    private static @Nullable MethodHandle getClassNameW;
    public static int GetClassNameW(MemorySegment hWnd, MemorySegment lpClassName, int nMaxCount) {
        if (getClassNameW == null) {
            getClassNameW = linker.downcallHandle(
                    user32.find("GetClassNameW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getClassNameW, hWnd, lpClassName, nMaxCount);
    }

    // =====================================================================
    // GetClassLongW
    // =====================================================================
    private static @Nullable MethodHandle getClassLongW;
    public static int GetClassLongW(MemorySegment hWnd, int nIndex) {
        if (getClassLongW == null) {
            getClassLongW = linker.downcallHandle(
                    user32.find("GetClassLongW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getClassLongW, hWnd, nIndex);
    }

    // =====================================================================
    // SetClassLongW
    // =====================================================================
    private static @Nullable MethodHandle setClassLongW;
    public static int SetClassLongW(MemorySegment hWnd, int nIndex, int dwNewLong) {
        if (setClassLongW == null) {
            setClassLongW = linker.downcallHandle(
                    user32.find("SetClassLongW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setClassLongW, hWnd, nIndex, dwNewLong);
    }

    // =====================================================================
    // GetClassLongPtrW
    // =====================================================================
    private static @Nullable MethodHandle getClassLongPtrW;
    public static long GetClassLongPtrW(MemorySegment hWnd, int nIndex) {
        if (getClassLongPtrW == null) {
            getClassLongPtrW = linker.downcallHandle(
                    user32.find("GetClassLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getClassLongPtrW, hWnd, nIndex);
    }

    // =====================================================================
    // SetClassLongPtrW
    // =====================================================================
    private static @Nullable MethodHandle setClassLongPtrW;
    public static long SetClassLongPtrW(MemorySegment hWnd, int nIndex, long dwNewLong) {
        if (setClassLongPtrW == null) {
            setClassLongPtrW = linker.downcallHandle(
                    user32.find("SetClassLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG
                    )
            );
        }

        return invoke(setClassLongPtrW, hWnd, nIndex, dwNewLong);
    }

    // =====================================================================
    // ScrollWindow
    // =====================================================================
    private static @Nullable MethodHandle scrollWindow;
    public static int ScrollWindow(MemorySegment hWnd, int XAmount, int YAmount, MemorySegment lpRect, MemorySegment lpClipRect) {
        if (scrollWindow == null) {
            scrollWindow = linker.downcallHandle(
                    user32.find("ScrollWindow").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(scrollWindow, hWnd, XAmount, YAmount, lpRect, lpClipRect);
    }

    // =====================================================================
    // ScrollWindowEx
    // =====================================================================
    private static @Nullable MethodHandle scrollWindowEx;
    public static int ScrollWindowEx(MemorySegment hWnd, int dx, int dy, MemorySegment prcScroll, MemorySegment prcClip, MemorySegment hrgnUpdate, MemorySegment prcUpdate, int flags) {
        if (scrollWindowEx == null) {
            scrollWindowEx = linker.downcallHandle(
                    user32.find("ScrollWindowEx").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(scrollWindowEx, hWnd, dx, dy, prcScroll, prcClip, hrgnUpdate, prcUpdate, flags);
    }

    // =====================================================================
    // SetScrollInfo
    // =====================================================================
    private static @Nullable MethodHandle setScrollInfo;
    public static int SetScrollInfo(MemorySegment hwnd, int nBar, MemorySegment lpsi, int redraw) {
        if (setScrollInfo == null) {
            setScrollInfo = linker.downcallHandle(
                    user32.find("SetScrollInfo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setScrollInfo, hwnd, nBar, lpsi, redraw);
    }

    // =====================================================================
    // GetScrollInfo
    // =====================================================================
    private static @Nullable MethodHandle getScrollInfo;
    public static int GetScrollInfo(MemorySegment hwnd, int nBar, MemorySegment lpsi) {
        if (getScrollInfo == null) {
            getScrollInfo = linker.downcallHandle(
                    user32.find("GetScrollInfo").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    )
            );
        }

        return invoke(getScrollInfo, hwnd, nBar, lpsi);
    }

    // =====================================================================
    // SetScrollPos
    // =====================================================================
    private static @Nullable MethodHandle setScrollPos;
    public static int SetScrollPos(MemorySegment hWnd, int nBar, int nPos, int bRedraw) {
        if (setScrollPos == null) {
            setScrollPos = linker.downcallHandle(
                    user32.find("SetScrollPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(setScrollPos, hWnd, nBar, nPos, bRedraw);
    }

    // =====================================================================
    // GetScrollPos
    // =====================================================================
    private static @Nullable MethodHandle getScrollPos;
    public static int GetScrollPos(MemorySegment hWnd, int nBar) {
        if (getScrollPos == null) {
            getScrollPos = linker.downcallHandle(
                    user32.find("GetScrollPos").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(getScrollPos, hWnd, nBar);
    }

    // =====================================================================
    // SetScrollRange
    // =====================================================================
    private static @Nullable MethodHandle setScrollRange;
    public static int SetScrollRange(MemorySegment hWnd, int nBar, int nMinPos, int nMaxPos, int bRedraw) {
        if (setScrollRange == null) {
            setScrollRange = linker.downcallHandle(
                    user32.find("SetScrollRange").orElseThrow(),
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

        return invoke(setScrollRange, hWnd, nBar, nMinPos, nMaxPos, bRedraw);
    }

    // =====================================================================
    // ShowScrollBar
    // =====================================================================
    private static @Nullable MethodHandle showScrollBar;
    public static int ShowScrollBar(MemorySegment hWnd, int wBar, int bShow) {
        if (showScrollBar == null) {
            showScrollBar = linker.downcallHandle(
                    user32.find("ShowScrollBar").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(showScrollBar, hWnd, wBar, bShow);
    }

    // =====================================================================
    // EnableScrollBar
    // =====================================================================
    private static @Nullable MethodHandle enableScrollBar;
    public static int EnableScrollBar(MemorySegment hWnd, int wSBflags, int wArrows) {
        if (enableScrollBar == null) {
            enableScrollBar = linker.downcallHandle(
                    user32.find("EnableScrollBar").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT
                    )
            );
        }

        return invoke(enableScrollBar, hWnd, wSBflags, wArrows);
    }
}
