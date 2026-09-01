package br.commons.cache;

import br.commons.Logger;
import br.commons.Result;
import br.commons.debug.Execution;
import br.commons.platform.NativeCache;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@NullMarked
public abstract class AbstractCache<T> {
    private static final long POPULATE_TIMEOUT_SECONDS = 2;

    private final String prefix;
    private final Function<String, List<T>> loader;
    private final Function<T, UUID> keyer;
    private final ToLongFunction<T> sizer;
    private final BiConsumer<MemorySegment, T> writer;
    
    private final ConcurrentHashMap<String, CompletableFuture<NativeCache>> registry = new ConcurrentHashMap<>();

    protected AbstractCache(String prefix,
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

    protected abstract T mapToDomain(MemorySegment segment);

    public CompletableFuture<Result<Void, String>> onLogin(String personId) {
        return Execution.nanos(() -> {
            val future = new CompletableFuture<NativeCache>();
            registry.put(personId, future);

            return CacheWorker.submitResult(() -> {
                val cache = new NativeCache();
                val errors = new ArrayList<String>();
                try {
                    for (val model : loader.apply(personId)) {
                        store(cache, model).ifFailure(errors::add);
                    }
                } finally {
                    future.complete(cache);
                }
                return errors.isEmpty()
                        ? Result.success()
                        : Result.failure("cache populate incomplete for " + personId + ": " + errors);
            });
        });
    }

    public CompletableFuture<Result<Void, String>> onLogout(String personId) {
        return Execution.nanos(() -> {
            val future = registry.remove(personId);
            if (future == null) return CompletableFuture.completedFuture(Result.success());

            return future.handle((cache, thrown) -> {
                if (thrown != null) return Result.failure(thrown.toString());
                try {
                    cache.close();
                    return Result.success();
                } catch (Exception e) {
                    Logger.warn("Cache close failed: %s", e.toString());
                    return Result.failure(e.toString());
                }
            });
        });
    }

    public CompletableFuture<Result<Void, String>> upsert(String personId, T model) {
        return Execution.nanos(() -> {
            return onCache(personId, cache -> store(cache, model));
        });
    }

    public CompletableFuture<Result<Void, String>> evict(String personId, UUID id) {
        return Execution.nanos(() -> {
            return onCache(personId, cache -> cache.remove(prefix + id));
        });
    }

    public CompletableFuture<Result<Void, String>> evictEverywhere(UUID id) {
        return Execution.nanos(() -> {
            val pending = registry.keySet().stream()
                    .map(personId -> onCache(personId, cache -> cache.remove(prefix + id)))
                    .toList();

            return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> pending.stream()
                            .map(CompletableFuture::join)
                            .filter(Result::isFailure)
                            .findFirst()
                            .orElse(Result.success()));
        });
    }

    public Result<Set<T>, String> list(String personId) {
        return Execution.nanos(() -> {
            return read(personId, Result.<Set<T>, String>success(Set.of()), cache -> {
                val set = new java.util.LinkedHashSet<T>();
                val traversal = cache.forEach(prefix, (key, seg) -> set.add(mapToDomain(seg)));
                if (traversal instanceof Result.Failure<Void, String> f) return Result.failure(f.error());
                return Result.success(set);
            });
        });
    }

    /** Nunca guarda/devolve {@code null}: ausência (cache não populado pra {@code personId} ou
     *  chave sem entrada) é sempre {@link Result.Failure}, nunca {@code Result.success(null)}. */
    public Result<T, String> find(String personId, UUID id) {
        return Execution.nanos(() -> {
            val notFound = Result.<T>failure("not found: " + prefix + id);
            return read(personId, notFound, cache -> {
                val segment = cache.get(prefix + id);
                return segment.isFailure() ? notFound : Result.success(mapToDomain(segment.get()));
            });
        });
    }

    private CompletableFuture<Result<Void, String>> onCache(String personId, Function<NativeCache, Result<Void, String>> action) {
        val future = registry.get(personId);
        if (future == null) return CompletableFuture.completedFuture(Result.success());

        return CacheWorker.submitResult(() -> {
            val cache = future.getNow(null);
            if (cache == null || cache.isClosed()) return Result.success();
            return action.apply(cache);
        });
    }

    private <R> Result<R, String> read(String personId, Result<R, String> onMiss, Function<NativeCache, Result<R, String>> body) {
        val future = registry.get(personId);
        if (future == null) return onMiss;

        try {
            val cache = future.get(POPULATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return cache.isClosed() ? onMiss : body.apply(cache);
        } catch (TimeoutException e) {
            Logger.debug("Cache populate timeout for %s", personId);
            return Result.failure("cache populate timeout for " + personId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure("interrupted reading cache for " + personId);
        } catch (Exception e) {
            Logger.warn("Cache read failed for %s: %s", personId, e.toString());
            return Result.failure(e.toString());
        }
    }

    private Result<Void, String> store(NativeCache cache, T model) {
        return cache.put(prefix + keyer.apply(model), sizer.applyAsLong(model))
                .flatMap(segment -> {
                    writer.accept(segment, model);
                    return Result.success();
                });
    }
}
