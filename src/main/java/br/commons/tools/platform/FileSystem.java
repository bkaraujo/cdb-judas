package br.commons.tools.platform;

import br.commons.Logger;
import br.commons.Result;
import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@NullMarked
public interface FileSystem {

    String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();
    String LINE_SEPARATOR = System.lineSeparator();

    /** JVM startup path */
    Path jvmFS();

    /** User home */
    Path userFS();

    static String read(Path path) {
        return Strings.read(path);
    }

    static void create(Path rootfs, String ... directories){
        Logger.trace("Creating directories %s", List.of(directories));
        if (!Files.exists(rootfs) || directories.length < 1) return;

        for (val directory : directories) {
            val name = Strings.lower(directory);

            try {
                Files.createDirectories(rootfs.resolve(name));
            } catch (FileAlreadyExistsException exception) {
                Logger.error(Strings.orEmpty(exception.getLocalizedMessage()), exception);
            } catch (IOException exception) {
                Logger.error("Failed to create directory", exception);
            } catch (Throwable throwable) {
                Logger.error("Unpredicted message", throwable);
            }
        }
    }

    static void copy(final byte [] bytes, final Path target) {
        Logger.trace("Copying %s to %s", bytes.length, target.toAbsolutePath());
        if (Files.isWritable(target)) {
            Logger.error("Path is not writable %s", target.toAbsolutePath());
            return;
        }

        try {
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            Logger.error("Fail to save %s", target.toAbsolutePath(), exception);
        } catch (Throwable throwable) {
            Logger.error("Unpredicted message", throwable);
        }
    }

    static Result<Path, Throwable> move(final Path source, final Path target) {
        Logger.trace("Moving %s to %s", source.toAbsolutePath(), target.toAbsolutePath());
        val resolved = Files.isDirectory(target) ? target.resolve(source.getFileName()) : target;

        try {
            // Try atomic move first (faster, safer)
            val result = Files.move(source, resolved, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return Result.success(result);
        } catch (AtomicMoveNotSupportedException | AccessDeniedException ex) {
            Logger.trace("Atomic move failed, falling back to copy+delete: %s", Strings.orEmpty(ex.getMessage()));
            try {
                Files.copy(source, resolved, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(source);
                return Result.success(resolved);
            } catch (IOException copyEx) {
                return Result.failure(copyEx);
            }
        } catch (Throwable throwable) {
            return Result.failure(throwable);
        }
    }

}
