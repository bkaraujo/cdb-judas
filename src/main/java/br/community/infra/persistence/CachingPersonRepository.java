package br.community.infra.persistence;

import br.community.context.people._0_domain.model.Person;
import br.community.context.people._0_domain.repository.PersonRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Decorador write-ahead de {@link PersonRepository}: espelho completo em memória, lock
 * compartilhado com {@link CachingUserRepository} (mesmo {@link ReadWriteLock}).
 */
@NullMarked
public final class CachingPersonRepository implements PersonRepository {

    private final PersonRepository delegate;
    private final ReadWriteLock lock;
    private final Map<UUID, Person> byId = new ConcurrentHashMap<>();

    public CachingPersonRepository(PersonRepository delegate, ReadWriteLock lock) {
        this.delegate = delegate;
        this.lock = lock;
        delegate.findAll().forEach(p -> byId.put(p.id(), p));
    }

    @Override
    public List<Person> findAll() {
        lock.readLock().lock();
        try { return new ArrayList<>(byId.values()); }
        finally { lock.readLock().unlock(); }
    }

    @Override
    public Optional<Person> findById(UUID id) {
        lock.readLock().lock();
        try { return Optional.ofNullable(byId.get(id)); }
        finally { lock.readLock().unlock(); }
    }

    @Override
    public Person save(Person entity) {
        lock.writeLock().lock();
        try {
            @Nullable Person previous = byId.get(entity.id());
            byId.put(entity.id(), entity);
            try {
                return delegate.save(entity);
            } catch (RuntimeException ex) {
                if (previous == null) byId.remove(entity.id()); else byId.put(entity.id(), previous);
                throw ex;
            }
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public void deleteById(UUID id) {
        lock.writeLock().lock();
        try {
            @Nullable Person previous = byId.remove(id);
            try {
                delegate.deleteById(id);
            } catch (RuntimeException ex) {
                if (previous != null) byId.put(id, previous);
                throw ex;
            }
        } finally { lock.writeLock().unlock(); }
    }

    @Override
    public void clearCache() {
        lock.writeLock().lock();
        try {
            byId.clear();
            delegate.findAll().forEach(p -> byId.put(p.id(), p));
        } finally { lock.writeLock().unlock(); }
    }
}
