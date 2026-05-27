package br.community.context.security._2_infrastructure;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.EntityDiff;
import br.community.context.security._0_domain.User;
import br.community.context.security._0_domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public final class UserJsonRepository implements UserRepository {

    private static final String AUTH_KEY = "auth";
    private static final String PASSWORD_KEY = "password";
    private static final long CACHE_TTL_MS = 60_000;

    private final Storage storage;
    private final ObjectMapper mapper;
    
    private final Map<String, CachedUser> cache = new ConcurrentHashMap<>();

    @NullMarked
    private record CachedUser(User user, long lastAccess) {}

    public UserJsonRepository(Storage storage, ObjectMapper mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    public Optional<User> findByUsername(String username) {
        val now = System.currentTimeMillis();
        val cached = cache.get(username);
        
        if (cached != null && (now - cached.lastAccess) < CACHE_TTL_MS) {
            cache.put(username, new CachedUser(cached.user, now));
            return Optional.of(cached.user);
        }

        val key = fileFor(username);
        if (!storage.exists(key)) return Optional.empty();

        try {
            val bytes = storage.read(key, AUTH_KEY);
            if (bytes == null) return Optional.empty();

            val auth = (ObjectNode) mapper.readTree(bytes);
            if (auth.get(PASSWORD_KEY) == null) return Optional.empty();

            val user = new User(username, auth.get(PASSWORD_KEY).asText());
            cache.put(username, new CachedUser(user, now));
            return Optional.of(user);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao ler user " + username, e);
        }
    }

    public User save(User user) {
        Logger.debug("Persisting user: %s", user.username());
        val before = findByUsername(user.username()).orElse(null);
        val key = fileFor(user.username());

        try {
            val auth = mapper.createObjectNode();
            auth.put(PASSWORD_KEY, user.password());
            storage.write(key, AUTH_KEY, mapper.writeValueAsBytes(auth));

            cache.put(user.username(), new CachedUser(user, System.currentTimeMillis()));
            Logger.verbose(() -> "auth/%s diff:%s".formatted(user.username(), EntityDiff.of(mapper, before, user)));
            return user;
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar user " + user.username(), e);
        }
    }

    private String fileFor(String username) {
        return username + ".json";
    }
}
