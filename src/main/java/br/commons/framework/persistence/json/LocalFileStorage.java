package br.commons.framework.persistence.json;

import br.commons.Logger;
import br.commons.Platform;
import br.commons.framework.persistence.Storage;
import br.community.core.JsonStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@NullMarked
public final class LocalFileStorage implements Storage {

    private final Path root;
    private final ObjectMapper mapper;

    public LocalFileStorage(JsonStorageProperties props, ObjectMapper mapper) {
        this.root = Platform.fileSystem().jvmFS().resolve(props.path());
        this.mapper = mapper;
        Logger.info("datafiles: %s", root);
    }

    @Override
    public byte @Nullable [] read(String file, String jsonKey) {
        val path = resolve(file);
        if (!Files.exists(path)) return null;

        try {
            val rootNode = mapper.readTree(path.toFile());
            val field = rootNode.get(jsonKey);
            if (field == null || field.isNull()) return null;

            return mapper.writeValueAsBytes(field);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao ler campo " + jsonKey + " em " + file, e);
        }
    }

    @Override
    public void write(String file, String jsonKey, byte[] content) {
        try {
            val path = resolve(file);
            Files.createDirectories(path.getParent());

            ObjectNode rootNode;
            if (Files.exists(path)) {
                rootNode = (ObjectNode) mapper.readTree(path.toFile());
            } else {
                rootNode = mapper.createObjectNode();
            }

            val newNode = mapper.readTree(content);
            rootNode.set(jsonKey, newNode);

            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rootNode);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar campo " + jsonKey + " em " + file, e);
        }
    }

    @Override
    public boolean exists(String file) {
        return Files.exists(resolve(file));
    }

    @Override
    public List<String> listFiles(String prefix, String suffix) {
        if (!Files.exists(root)) return List.of();
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(prefix) && n.endsWith(suffix))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao listar arquivos em " + root, e);
        }
    }

    private Path resolve(String file) {
        return root.resolve(file);
    }
}
