package br.cdb.core.cache;

import br.commons.Logger;
import br.commons.Result;
import br.commons.platform.NativeCache;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Registry genérico por pessoa, parametrizado por lambdas. Armazena cache off-heap
 * sincronizando com eventos de sessão: populate no login, close no logout.
 * Miss = lista vazia (D1). Populate é assíncrono mas o future é registrado na thread
 * chamadora para D1-b (corrida de login).
 *
 * <p>API pública em {@link Result}: <b>ausência de sessão não é falha</b> (é o miss do D1, que
 * devolve sucesso vazio) — falha é o cache indisponível de verdade: timeout do populate, worker
 * recusando tarefa, arena já fechada. As mutações devolvem o {@code Result} dentro de um
 * {@link CompletableFuture} (o veredito nasce na thread do worker, ver {@link CacheWorker});
 * as leituras, que já bloqueiam no populate, devolvem o {@code Result} direto.
 */
@NullMarked
public class SessionScopedCache<T> {
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

    public CompletableFuture<Result<Void, String>> onLogin(String personId) {
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
                // Completa sempre: o que carregou vale mais que uma sessão sem cache nenhum.
                future.complete(cache);
            }
            return errors.isEmpty()
                    ? Result.success()
                    : Result.failure("cache populate incomplete for " + personId + ": " + errors);
        });
    }

    public CompletableFuture<Result<Void, String>> onLogout(String personId) {
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
    }

    public CompletableFuture<Result<Void, String>> upsert(String personId, T model) {
        return onCache(personId, cache -> store(cache, model));
    }

    public CompletableFuture<Result<Void, String>> evict(String personId, UUID id) {
        return onCache(personId, cache -> cache.remove(prefix + id));
    }

    public CompletableFuture<Result<Void, String>> evictEverywhere(UUID id) {
        val pending = registry.keySet().stream()
                .map(personId -> onCache(personId, cache -> cache.remove(prefix + id)))
                .toList();

        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> pending.stream()
                        .map(CompletableFuture::join)
                        .filter(Result::isFailure)
                        .findFirst()
                        .orElse(Result.success()));
    }

    /**
     * Percorre os registros da pessoa. Sem sessão é sucesso vazio (D1). Bloqueia até 2s no populate
     * (D1-b) e falha se estourar.
     */
    public Result<Void, String> forEach(String personId, Consumer<MemorySegment> consumer) {
        return read(personId, Result.success(), cache -> cache.forEach(prefix, (key, seg) -> consumer.accept(seg)));
    }

    /** Sucesso com {@code true} se achou (o consumer recebeu o segmento); falha só se o cache não respondeu. */
    public Result<Boolean, String> find(String personId, UUID id, Consumer<MemorySegment> consumer) {
        return read(personId, Result.success(false), cache -> {
            val segment = cache.get(prefix + id);
            if (segment.isFailure()) return Result.success(false);

            consumer.accept(segment.get());
            return Result.success(true);
        });
    }

    /**
     * Roda {@code action} na thread do worker, sobre o cache já populado da pessoa. Sem sessão, ou
     * com a arena fechada (corrida com o logout), é no-op bem-sucedido — não há o que manter.
     */
    private CompletableFuture<Result<Void, String>> onCache(String personId, Function<NativeCache, Result<Void, String>> action) {
        val future = registry.get(personId);
        if (future == null) return CompletableFuture.completedFuture(Result.success());

        return CacheWorker.submitResult(() -> {
            val cache = future.getNow(null);
            if (cache == null || cache.isClosed()) return Result.success();
            return action.apply(cache);
        });
    }

    /**
     * Espera o populate e entrega o cache a {@code body}. Concentra a semântica de leitura: sem
     * sessão (ou arena fechada) devolve {@code onMiss}; só o cache que não respondeu vira falha.
     */
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

    /** Aloca o segmento do modelo e escreve nele — o único ponto que conhece prefix/keyer/sizer/writer. */
    private Result<Void, String> store(NativeCache cache, T model) {
        return cache.put(prefix + keyer.apply(model), sizer.applyAsLong(model))
                .flatMap(segment -> {
                    writer.accept(segment, model);
                    return Result.success();
                });
    }
}
