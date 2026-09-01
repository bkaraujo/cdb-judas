package br.commons.cache;

import br.commons.Logger;
import br.commons.Result;
import br.commons.chrono.Time;
import br.commons.debug.Execution;
import br.commons.framework.logger.LogLevel;
import br.commons.platform.NativeCache;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Cache off-heap por sessão. As consultas ({@link #find}, {@link #list}) são caminho quente: código
 * reto, sem lambda, sem montar chave e sem instrumentação — o cronômetro só existe se DEBUG estiver
 * ligado para esta classe. As mutações continuam serializadas no {@link CacheWorker} e cronometradas
 * por {@link Execution}, que é barato quando o nível não deixa passar.
 */
@NullMarked
public abstract class AbstractCache<T> {
    private static final long POPULATE_TIMEOUT_SECONDS = 2;

    /** Classe do gate de log: constante, para o {@link Logger#enabled(LogLevel, String)} não ter
     *  de varrer a pilha atrás do chamador a cada consulta. */
    private static final String CALLER = AbstractCache.class.getName();

    private final String prefix;
    private final Function<String, List<T>> loader;
    private final Function<T, UUID> keyer;
    private final ToLongFunction<T> sizer;
    private final BiConsumer<MemorySegment, T> writer;

    private final ConcurrentHashMap<String, CompletableFuture<NativeCache<UUID>>> registry = new ConcurrentHashMap<>();

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
        return Execution.nanos(CALLER, () -> {
            val future = new CompletableFuture<NativeCache<UUID>>();
            registry.put(personId, future);

            return CacheWorker.submitResult(() -> {
                val cache = new NativeCache<UUID>();
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
        return Execution.nanos(CALLER, () -> {
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
        return Execution.nanos(CALLER, () -> {
            return onCache(personId, cache -> store(cache, model));
        });
    }

    public CompletableFuture<Result<Void, String>> evict(String personId, UUID id) {
        return Execution.nanos(CALLER, () -> {
            return onCache(personId, cache -> cache.remove(id));
        });
    }

    public CompletableFuture<Result<Void, String>> evictEverywhere(UUID id) {
        return Execution.nanos(CALLER, () -> {
            val pending = registry.keySet().stream()
                    .map(personId -> onCache(personId, cache -> cache.remove(id)))
                    .toList();

            return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> pending.stream()
                            .map(CompletableFuture::join)
                            .filter(Result::isFailure)
                            .findFirst()
                            .orElse(Result.success()));
        });
    }

    /**
     * Lista na ordem de inserção do cache. Devolve {@code List} — e não {@code Set} — porque a
     * unicidade já vem da chave: um {@code LinkedHashSet} aqui hasheava cada record de domínio
     * inteiro para deduplicar o que não duplica, e todo chamador copiava o resultado para um
     * {@code ArrayList} logo em seguida. A lista é nova a cada chamada, e do chamador.
     */
    public Result<List<T>, String> list(String personId) {
        if (!Logger.enabled(LogLevel.DEBUG, CALLER)) return doList(personId);

        val start = Time.nanos();
        try {
            return doList(personId);
        } finally {
            Logger.debug("list time: %s ns", Time.nanos() - start);
        }
    }

    /** Nunca guarda/devolve {@code null}: ausência (cache não populado pra {@code personId} ou
     *  chave sem entrada) é sempre {@link Result.Failure}, nunca {@code Result.success(null)}. */
    public Result<T, String> find(String personId, UUID id) {
        if (!Logger.enabled(LogLevel.DEBUG, CALLER)) return doFind(personId, id);

        val start = Time.nanos();
        try {
            return doFind(personId, id);
        } finally {
            Logger.debug("find time: %s ns", Time.nanos() - start);
        }
    }

    private Result<List<T>, String> doList(String personId) {
        val cache = live(personId);
        if (cache != null) return collect(cache);

        return switch (awaited(personId)) {
            case Result.Failure(var error) -> Result.failure(error);
            case Result.Success(var resolved) -> resolved == null
                    ? Result.success(List.of())
                    : collect(resolved);
        };
    }

    private Result<T, String> doFind(String personId, UUID id) {
        val cache = live(personId);
        if (cache != null) return lookup(cache, id);

        return switch (awaited(personId)) {
            case Result.Failure(var error) -> Result.failure(error);
            case Result.Success(var resolved) -> resolved == null
                    ? notFound(id)
                    : lookup(resolved, id);
        };
    }

    private Result<T, String> lookup(NativeCache<UUID> cache, UUID id) {
        val segment = cache.segment(id);
        return segment == null ? notFound(id) : Result.success(mapToDomain(segment));
    }

    private Result<List<T>, String> collect(NativeCache<UUID> cache) {
        val models = new ArrayList<T>(cache.count());
        val traversal = cache.forEach((_, segment) -> models.add(mapToDomain(segment)));
        if (traversal instanceof Result.Failure<Void, String> f) return Result.failure(f.error());
        return Result.success(models);
    }

    /** Só no miss: a mensagem custa um {@code UUID.toString} e uma concatenação, e é aqui que o
     *  {@code prefix} sobrevive — na chave ele era redundante, cada cache tem só o próprio tipo. */
    private Result<T, String> notFound(UUID id) {
        return Result.failure("not found: " + prefix + id);
    }

    /** Cache pronto para leitura imediata, ou {@code null} se ausente, ainda populando ou fechado. */
    private @Nullable NativeCache<UUID> live(String personId) {
        val future = registry.get(personId);
        if (future == null) return null;

        val cache = future.getNow(null);
        return cache == null || cache.isClosed() ? null : cache;
    }

    /**
     * Caminho raro, fora do hotpath: pessoa sem cache, ou populate ainda em voo logo depois do
     * login. Pode esperar, alocar e falhar. {@code Success} com {@code null} é ausência — o
     * chamador decide o que ausência significa para ele.
     */
    private Result<@Nullable NativeCache<UUID>, String> awaited(String personId) {
        val future = registry.get(personId);
        if (future == null) return Result.success(null);

        try {
            val cache = future.get(POPULATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return cache.isClosed() ? Result.success(null) : Result.success(cache);
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

    private CompletableFuture<Result<Void, String>> onCache(String personId, Function<NativeCache<UUID>, Result<Void, String>> action) {
        val future = registry.get(personId);
        if (future == null) return CompletableFuture.completedFuture(Result.success());

        return CacheWorker.submitResult(() -> {
            val cache = future.getNow(null);
            if (cache == null || cache.isClosed()) return Result.success();
            return action.apply(cache);
        });
    }

    private Result<Void, String> store(NativeCache<UUID> cache, T model) {
        return cache.put(keyer.apply(model), sizer.applyAsLong(model))
                .flatMap(segment -> {
                    writer.accept(segment, model);
                    return Result.success();
                });
    }
}
