package br.commons.framework.persistence.inmemory;

import br.commons.framework.persistence.Storage;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@NullMarked
public class InMemoryStorage implements Storage {

    private final Map<String, Map<String, byte[]>> data = new ConcurrentHashMap<>();

    @Override
    public byte @Nullable [] read(String file, String jsonKey) {
        val fileContent = data.get(file);
        if (fileContent != null) {
            return fileContent.get(jsonKey);
        }
        return null;
    }

    @Override
    public void write(String file, String jsonKey, byte[] content) {
        data.computeIfAbsent(file, k -> new ConcurrentHashMap<>()).put(jsonKey, content);
    }

    @Override
    public boolean exists(String file) {
        return data.containsKey(file);
    }

    @Override
    public List<String> listFiles(String prefix, String suffix) {
        return data.keySet().stream()
                .filter(name -> (prefix == null || name.startsWith(prefix)) && (suffix == null || name.endsWith(suffix)))
                .collect(Collectors.toList());
    }

    public void clear() {
        data.clear();
    }
}
