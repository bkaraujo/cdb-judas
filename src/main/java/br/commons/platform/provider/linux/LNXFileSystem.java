package br.commons.platform.provider.linux;

import br.commons.platform.FileSystem;
import br.commons.tools.Strings;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public class LNXFileSystem implements FileSystem {

    @Override
    public Path jvmFS() {
        return Path.of(Strings.EMPTY).toAbsolutePath();
    }

    @Override
    public Path userFS() {
        return Path.of(System.getProperty("user.home"));
    }

}
