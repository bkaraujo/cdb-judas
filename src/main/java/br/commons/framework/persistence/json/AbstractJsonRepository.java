package br.commons.framework.persistence.json;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import br.community.core.web.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NullMarked
public abstract class AbstractJsonRepository<T, ID> implements Repository<T, ID> {

    private static final long CACHE_TTL_MS = 60_000;

    private final ObjectMapper mapper;
    private final Storage storage;
    private final Class<T> type;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Nullable
    private volatile List<T> cachedData;
    private volatile long lastAccess;
    @Nullable
    private volatile String lastUser;

    protected AbstractJsonRepository(ObjectMapper mapper, Storage storage, Class<T> type) {
        this.mapper = mapper;
        this.storage = storage;
        this.type = type;
    }

    protected String fileName() {
        return CurrentUser.getId() + ".json";
    }

    protected abstract String jsonKey();

    protected abstract ID getId(T entity);

    @Override
    public List<T> findAll() {
        Logger.debug("[%s] Querying: %s", fileName(), jsonKey());
        return readAll();
    }

    @Override
    public Optional<T> findById(ID id) {
        Logger.debug("[%s] Querying %s from: %s", fileName(), id, jsonKey());
        return readAll().stream()
                .filter(e -> getId(e).equals(id))
                .findFirst();
    }

    @Override
    public T save(T entity) {
        lock.writeLock().lock();
        try {
            Logger.debug("[%s] Saving %s from: %s", fileName(), entity, jsonKey());
            val all = new ArrayList<>(readAll());
            val before = all.stream().filter(e -> getId(e).equals(getId(entity))).findFirst().orElse(null);

            Logger.verbose(() -> "[%s] %s/%s diff:%s".formatted(fileName(), jsonKey(), getId(entity), EntityDiff.of(mapper, before, entity)));
            all.removeIf(e -> getId(e).equals(getId(entity)));
            all.add(entity);
            writeAll(all);
            return entity;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteById(ID id) {
        lock.writeLock().lock();
        try {
            Logger.debug("[%s] Deleting %s from: %s", fileName(), id, jsonKey());
            val all = new ArrayList<>(readAll());
            all.removeIf(e -> getId(e).equals(id));
            writeAll(all);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clearCache() {
        lock.writeLock().lock();
        try {
            cachedData = null;
            lastAccess = 0;
            lastUser = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<T> readAll() {
        val now = System.currentTimeMillis();
        val username = CurrentUser.getId();

        lock.readLock().lock();
        try {
            if (cachedData != null && username.equals(lastUser) && (now - lastAccess) < CACHE_TTL_MS) {
                lastAccess = now;
                return cachedData;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            // Re-check after lock
            if (cachedData != null && username.equals(lastUser) && (now - lastAccess) < CACHE_TTL_MS) {
                lastAccess = now;
                return cachedData;
            }

            val bytes = storage.read(fileName(), jsonKey());
            List<T> data;
            if (bytes == null || bytes.length == 0) {
                data = new ArrayList<>();
            } else {
                val listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
                data = mapper.readValue(bytes, listType);
            }
            cachedData = Collections.unmodifiableList(data);
            lastAccess = now;
            lastUser = username;
            return this.cachedData;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading " + fileName() + ":" + jsonKey(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void writeAll(List<T> entities) {
        try {
            storage.write(fileName(), jsonKey(), mapper.writeValueAsBytes(entities));
            cachedData = List.copyOf(entities);
            lastAccess = System.currentTimeMillis();
            lastUser = CurrentUser.getId();
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing " + fileName() + ":" + jsonKey(), e);
        }
    }
}
