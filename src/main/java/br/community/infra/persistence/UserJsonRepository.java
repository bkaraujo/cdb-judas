package br.community.infra.persistence;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NullMarked
public final class UserJsonRepository implements UserRepository {

    private static final String FILE = "users.json";
    private static final String KEY = "users";
    private static final long CACHE_TTL_MS = 60_000;

    private final Storage storage;
    private final ObjectMapper mapper;

    @Nullable
    private volatile List<User> cache;
    private volatile long loadedAt;

    public UserJsonRepository(Storage storage, ObjectMapper mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return all().stream().filter(u -> u.username().equals(username)).findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return all().stream().filter(u -> u.id().equals(id)).findFirst();
    }

    @Override
    public synchronized User save(User user) {
        Logger.debug("Persisting user: %s (%s)", user.username(), user.id());
        val all = new ArrayList<>(all());
        all.removeIf(u -> u.id().equals(user.id()));
        all.add(user);
        try {
            storage.write(FILE, KEY, mapper.writeValueAsBytes(all));
        } catch (JacksonException e) {
            throw new RuntimeException("Erro ao gravar user " + user.username(), e);
        }
        cache = List.copyOf(all);
        loadedAt = System.currentTimeMillis();
        return user;
    }

    private List<User> all() {
        val now = System.currentTimeMillis();
        val cached = cache;
        if (cached != null && (now - loadedAt) < CACHE_TTL_MS) return cached;

        val bytes = storage.read(FILE, KEY);
        List<User> list;
        if (bytes == null || bytes.length == 0) {
            list = List.of();
        } else {
            try {
                val listType = mapper.getTypeFactory().constructCollectionType(List.class, User.class);
                list = List.copyOf(mapper.readValue(bytes, listType));
            } catch (JacksonException e) {
                throw new RuntimeException("Erro ao ler registro de usuários", e);
            }
        }
        cache = list;
        loadedAt = now;
        return list;
    }
}
