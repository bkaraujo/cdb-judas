package br.commons.tools;

import br.commons.Logger;
import br.commons.tools.platform.FileSystem;
import br.commons.tools.platform.Network;
import br.commons.tools.platform.OS;
import br.commons.tools.platform.Terminal;
import br.commons.tools.platform.provider.linux.LNXFileSystem;
import br.commons.tools.platform.provider.linux.LNXNetwork;
import br.commons.tools.platform.provider.linux.LNXTerminal;
import br.commons.tools.platform.provider.windows.WINFileSystem;
import br.commons.tools.platform.provider.windows.WINNetwork;
import br.commons.tools.platform.provider.windows.WINTerminal;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class Platform {

    private Platform(){}


    public static final OS CURRENT;
    static {
        val osName = Strings.lower(System.getProperty("os.name"));
        if (osName.contains("win")) {
            CURRENT = OS.WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            CURRENT = OS.LINUX;
        } else if (osName.contains("mac")) {
            CURRENT = OS.MAC;
        } else {
            CURRENT = OS.UNKNOWN;
        }
    }

    private static @Nullable FileSystem fileSystem;
    @SuppressWarnings("NullAway")
    public static FileSystem fileSystem() {
        if (fileSystem == null) {
            fileSystem = switch (CURRENT) {
                case LINUX -> new LNXFileSystem();
                case WINDOWS -> new WINFileSystem();
                default -> null;
            };

            if (fileSystem == null) {
                Logger.fatal("Unsupported platform: %s", CURRENT);
            }
        }

        return fileSystem;
    }

    private static @Nullable Terminal terminal;
    @SuppressWarnings("NullAway")
    public static Terminal terminal() {
        if (terminal == null) {
            terminal = switch (CURRENT) {
                case LINUX -> new LNXTerminal();
                case WINDOWS -> new WINTerminal();
                default -> null;
            };

            if (terminal == null) {
                Logger.fatal("Unsupported platform: %s", CURRENT);
            }
        }

        return terminal;
    }

    private static @Nullable Network network;
    @SuppressWarnings("NullAway")
    public static Network network() {
        if (network == null) {
            network = switch (CURRENT) {
                case LINUX -> new LNXNetwork();
                case WINDOWS -> new WINNetwork();
                default -> null;
            };

            if (network == null) {
                Logger.fatal("Unsupported platform: %s", CURRENT);
            }
        }

        return network;
    }

    /** Verifica se a porta TCP já está em uso no host local. Ver {@link Network#isPortInUse(int)}. */
    public static boolean isPortInUse(int port) {
        return network().isPortInUse(port);
    }

}
