package br.cdb.feature.f000._2_infrastructure.persistence;

import br.cdb.core.persistence.AbstractCachingRepository;
import br.cdb.core.persistence.CachingUserRepository;
import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Decorador write-ahead de {@link PersonRepository}: espelho completo em memória, lock
 * compartilhado com {@link CachingUserRepository} (mesmo {@link ReadWriteLock}).
 */
@NullMarked
public final class CachingPersonRepository extends AbstractCachingRepository implements PersonRepository {

    private final PersonRepository delegate;
    private final Map<UUID, Person> byId = new ConcurrentHashMap<>();

    public CachingPersonRepository(PersonRepository delegate) {
        this.delegate = delegate;
        delegate.findAll().forEach(person -> byId.put(person.id(), person));
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
            val previous = byId.get(entity.id());
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
            val previous = byId.remove(id);
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
