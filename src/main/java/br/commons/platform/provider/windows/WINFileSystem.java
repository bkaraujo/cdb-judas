package br.commons.platform.provider.windows;

import br.commons.Logger;
import br.commons.platform.FileSystem;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

/**
 * Windows implementation of FileSystem using Java 25 FFM.
 * Calls shell32.dll directly via Foreign Function & Memory API.
 */
@NullMarked
public class WINFileSystem implements FileSystem {

    @Override
    public Path userFS() {
        var path = getFolderPath(WinAPI.ShellObj.CSIDL_PERSONAL);
        if (path == null) {
            Logger.warn("Failed to get CSIDL_PERSONAL, falling back to user.home");
            path = System.getProperty("user.home");
        }

        return Path.of(path + PATH_SEPARATOR);
    }

    @Override
    public Path jvmFS() {
        return Path.of(Strings.EMPTY).toAbsolutePath();
    }

    /**
     * Gets a special folder path using SHGetFolderPathW.
     *
     * @param csidl The CSIDL constant identifying the folder
     * @return The folder path, or null if the call failed
     */
    private @Nullable String getFolderPath(int csidl) {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate buffer for wide characters (2 bytes per char)
            val pathBuffer = arena.allocate(WinAPI.MAX_PATH * 2L);
            val result = Shell32.SHGetFolderPathW(
                    MemorySegment.NULL,  // hwnd
                    csidl,               // csidl
                    MemorySegment.NULL,  // hToken
                    WinAPI.ShellObj.SHGFP_TYPE_CURRENT,  // dwFlags
                    pathBuffer           // pszPath
            );

            if (result != WinAPI.S_OK) {
                Logger.warn("SHGetFolderPathW failed with HRESULT: 0x%08X", result);
                return null;
            }

            // Read the wide string from the buffer
            return WinAPI.readWideString(pathBuffer);

        } catch (Throwable e) {
            Logger.error("Exception calling SHGetFolderPathW: %s", Strings.orEmpty(e.getMessage()));
            return null;
        }
    }

}
