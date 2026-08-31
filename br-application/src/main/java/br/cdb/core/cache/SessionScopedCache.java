package br.cdb.core.cache;

import br.commons.platform.NativeCache;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.logging.Logger;

/**
 * Registry genérico por pessoa, parametrizado por lambdas. Armazena cache off-heap
 * sincronizando com eventos de sessão: populate no login, close no logout.
 * Miss = lista vazia (D1). Populate é assíncrono mas o future é registrado na thread
 * chamadora para D1-b (corrida de login).
 */
@NullMarked
public class SessionScopedCache<T> {
    private static final Logger log = Logger.getLogger(SessionScopedCache.class.getName());
    private static final long POPULATE_TIMEOUT_SECONDS = 2;

    private final String prefix;
    private final Function<String, List<T>> loader;
    private final Function<T, UUID> keyer;
    private final ToLongFunction<T> sizer;
    private final BiConsumer<MemorySegment, T> writer;
    private final ConcurrentHashMap<String, CompletableFuture<NativeCache>> registry = new ConcurrentHashMap<>();

    public SessionScopedCache(String prefix,
                              Function<String, List<T>> loader,
                              Function<T, UUID> keyer,
                              ToLongFunction<T> sizer,
                              BiConsumer<MemorySegment, T> writer) {
        this.prefix = prefix;
        this.loader = loader;
        this.keyer = keyer;
        this.sizer = sizer;
        this.writer = writer;
    }

    public void onLogin(String personId) {
        val future = new CompletableFuture<NativeCache>();
        registry.put(personId, future);

        CacheWorker.submit(() -> {
            try {
                val cache = new NativeCache();
                val models = loader.apply(personId);
                for (val model : models) {
                    val id = keyer.apply(model);
                    val key = prefix + id;
                    val size = sizer.applyAsLong(model);
                    val seg = cache.put(key, size);
                    writer.accept(seg, model);
                }
                future.complete(cache);
            } catch (Exception e) {
                log.warning("Cache populate failed for " + personId + ": " + e.getMessage());
                future.complete(new NativeCache());
            }
        });
    }

    public void onLogout(String personId) {
        val future = registry.remove(personId);
        if (future != null) {
            future.thenAccept(cache -> {
                try {
                    cache.close();
                } catch (Exception e) {
                    log.warning("Cache close failed: " + e.getMessage());
                }
            });
        }
    }

    public void upsert(String personId, T model) {
        val future = registry.get(personId);
        if (future == null) return;

        CacheWorker.submit(() -> {
            try {
                val cache = future.getNow(null);
                if (cache == null || cache.isClosed()) return;
                val id = keyer.apply(model);
                val key = prefix + id;
                val size = sizer.applyAsLong(model);
                val seg = cache.put(key, size);
                writer.accept(seg, model);
            } catch (Exception e) {
                log.warning("Cache upsert failed for " + personId + ": " + e.getMessage());
            }
        });
    }

    public void evict(String personId, UUID id) {
        val future = registry.get(personId);
        if (future == null) return;

        CacheWorker.submit(() -> {
            try {
                val cache = future.getNow(null);
                if (cache == null || cache.isClosed()) return;
                val key = prefix + id;
                cache.remove(key);
            } catch (Exception e) {
                log.warning("Cache evict failed for " + personId + ": " + e.getMessage());
            }
        });
    }

    public void evictEverywhere(UUID id) {
        for (val future : registry.values()) {
            CacheWorker.submit(() -> {
                try {
                    val cache = future.getNow(null);
                    if (cache == null || cache.isClosed()) return;
                    val key = prefix + id;
                    cache.remove(key);
                } catch (Exception e) {
                    log.warning("Cache evict-everywhere failed: " + e.getMessage());
                }
            });
        }
    }

    /** Percorre os registros da pessoa. Vazio se não há sessão (D1). Bloqueia até 2s no populate (D1-b). */
    public void forEach(String personId, Consumer<MemorySegment> consumer) {
        val future = registry.get(personId);
        if (future == null) return;

        try {
            val cache = future.get(POPULATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (cache.isClosed()) return;

            for (val key : cache.keys(prefix)) {
                val seg = cache.get(key);
                if (seg != null) {
                    consumer.accept(seg);
                }
            }
        } catch (java.util.concurrent.TimeoutException e) {
            log.fine("Cache populate timeout for " + personId);
        } catch (Exception e) {
            log.warning("Cache forEach failed: " + e.getMessage());
        }
    }

    /** true se achou; o consumer recebe o segmento. */
    public boolean find(String personId, UUID id, Consumer<MemorySegment> consumer) {
        val future = registry.get(personId);
        if (future == null) return false;

        try {
            val cache = future.get(POPULATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (cache.isClosed()) return false;

            val key = prefix + id;
            val seg = cache.get(key);
            if (seg != null) {
                consumer.accept(seg);
                return true;
            }
            return false;
        } catch (java.util.concurrent.TimeoutException e) {
            log.fine("Cache populate timeout for " + personId);
            return false;
        } catch (Exception e) {
            log.warning("Cache find failed: " + e.getMessage());
            return false;
        }
    }
}
