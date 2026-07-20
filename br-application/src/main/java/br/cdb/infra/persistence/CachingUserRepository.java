package br.cdb.infra.persistence;

import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorador write-ahead de {@link UserRepository}: cache lazy por id e username,
 * double-check sob writeLock no miss, lock compartilhado com {@link CachingPersonRepository}.
 */
@NullMarked
public final class CachingUserRepository extends AbstractCachingRepository implements UserRepository {

    private final UserRepository delegate;
    private final Map<String, User> byId = new ConcurrentHashMap<>();
    private final Map<String, String> usernameIndex = new ConcurrentHashMap<>();

    public CachingUserRepository(UserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            val cached = byId.get(id);
            if (cached != null) return Optional.of(cached);
        } finally { lock.readLock().unlock(); }

        lock.writeLock().lock();
        try {
            val cached = byId.get(id);
            if (cached != null) return Optional.of(cached);
            val result = delegate.findById(id);
            result.ifPresent(this::index);
            return result;
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        lock.readLock().lock();
        try {
            val id = usernameIndex.get(username);
            if (id != null) {
                val cached = byId.get(id);
                if (cached != null) return Optional.of(cached);
            }
        } finally { lock.readLock().unlock(); }

        lock.writeLock().lock();
        try {
            val id = usernameIndex.get(username);
            if (id != null) {
                val cached = byId.get(id);
                if (cached != null) return Optional.of(cached);
            }
            val result = delegate.findByUsername(username);
            result.ifPresent(this::index);
            return result;
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public Optional<User> findByPersonId(String personId) {
        // Baixa frequência (só /api/me): sem índice dedicado por pessoa — delega e indexa por id/username.
        val result = delegate.findByPersonId(personId);
        result.ifPresent(this::index);
        return result;
    }

    @Override
    public User save(User user) {
        lock.writeLock().lock();
        try {
            val prevById = byId.get(user.id());
            val prevUsername = usernameIndex.get(user.username());
            try {
                // Indexa o resultado do delegate (que carrega o personId persistido), não a entrada
                // — os construtores de criação passam personId nulo e cachear isso vazaria null.
                val saved = delegate.save(user);
                index(saved);
                return saved;
            } catch (RuntimeException ex) {
                if (prevById == null) byId.remove(user.id()); else byId.put(user.id(), prevById);
                if (prevUsername == null) usernameIndex.remove(user.username()); else usernameIndex.put(user.username(), prevUsername);
                throw ex;
            }
        } finally { lock.writeLock().unlock(); }
    }

    private void index(User user) {
        byId.put(user.id(), user);
        usernameIndex.put(user.username(), user.id());
    }
}
