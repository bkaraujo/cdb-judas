package br.community.infra.persistence;

import br.community.core.web.security.User;
import br.community.core.web.security.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Decorador write-ahead de {@link UserRepository}: cache lazy por id e username,
 * double-check sob writeLock no miss, lock compartilhado com {@link CachingPersonRepository}.
 */
@NullMarked
public final class CachingUserRepository implements UserRepository {

    private final UserRepository delegate;
    private final ReadWriteLock lock;
    private final Map<String, User> byId = new ConcurrentHashMap<>();
    private final Map<String, String> usernameIndex = new ConcurrentHashMap<>();

    public CachingUserRepository(UserRepository delegate, ReadWriteLock lock) {
        this.delegate = delegate;
        this.lock = lock;
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            @Nullable User cached = byId.get(id);
            if (cached != null) return Optional.of(cached);
        } finally { lock.readLock().unlock(); }

        lock.writeLock().lock();
        try {
            @Nullable User cached = byId.get(id);
            if (cached != null) return Optional.of(cached);
            Optional<User> result = delegate.findById(id);
            result.ifPresent(this::index);
            return result;
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        lock.readLock().lock();
        try {
            @Nullable String id = usernameIndex.get(username);
            if (id != null) {
                @Nullable User cached = byId.get(id);
                if (cached != null) return Optional.of(cached);
            }
        } finally { lock.readLock().unlock(); }

        lock.writeLock().lock();
        try {
            @Nullable String id = usernameIndex.get(username);
            if (id != null) {
                @Nullable User cached = byId.get(id);
                if (cached != null) return Optional.of(cached);
            }
            Optional<User> result = delegate.findByUsername(username);
            result.ifPresent(this::index);
            return result;
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public User save(User user) {
        lock.writeLock().lock();
        try {
            @Nullable User prevById = byId.get(user.id());
            @Nullable String prevUsername = usernameIndex.get(user.username());
            index(user);
            try {
                return delegate.save(user);
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
