package br.commons.cache;

import br.commons.Logger;
import br.commons.Result;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Thread única que serializa toda a manutenção do cache off-heap. A API pública devolve
 * {@link Result} em vez de lançar: quem publica o evento está dentro da transação dele e não pode
 * ser derrubado por uma falha de cache.
 *
 * <p>O veredito vem <b>dentro</b> de um {@link CompletableFuture} porque a exceção da tarefa só
 * existe depois, na thread do worker: devolver um {@code Result} síncrono seria sucesso falso.
 * Quem só enfileira ignora o future; quem precisa saber (testes, diagnóstico) espera por ele.
 */
@NullMarked
public final class CacheWorker {
    private static final long AWAIT_SECONDS = 10;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(
            r -> { val t = new Thread(r, "cache-worker"); t.setDaemon(true); return t; });

    private CacheWorker() {}

    /** Nunca deixa exceção escapar: o publicador do evento está dentro da transação dele. */
    public static CompletableFuture<Result<Void, String>> submit(Runnable task) {
        try {
            return CompletableFuture.runAsync(task, WORKER).handle(CacheWorker::verdict);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.completedFuture(rejected(e));
        }
    }

    /**
     * Como {@link #submit}, para a tarefa que já sabe dizer se deu certo: o {@code Result} dela
     * atravessa intacto, sem lançar uma exceção só para o {@link #verdict} reconstruir a mensagem.
     */
    public static CompletableFuture<Result<Void, String>> submitResult(Supplier<Result<Void, String>> task) {
        return supply(task).thenApply(outcome -> outcome.flatMap(inner -> inner));
    }

    public static <T> CompletableFuture<Result<T, String>> supply(Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, WORKER).handle(CacheWorker::verdict);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.completedFuture(rejected(e));
        }
    }

    public static Result<Void, String> awaitIdle() {
        WORKER.shutdown();
        try {
            if (!WORKER.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                WORKER.shutdownNow();
                return Result.failure("cache worker did not terminate in " + AWAIT_SECONDS + "s");
            }
            return Result.success();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            WORKER.shutdownNow();
            return Result.failure(e.toString());
        }
    }

    /**
     * Espera todas as tarefas pending terminarem SEM desligar o worker (para testes). A tarefa
     * marcadora é FIFO: quando ela roda, todas as anteriores já rodaram.
     */
    public static Result<Void, String> waitForPending() {
        try {
            return submit(() -> {}).get(AWAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(e.toString());
        } catch (ExecutionException | TimeoutException e) {
            return Result.failure("cache worker still busy: " + e);
        }
    }

    /** Converte o par (valor, exceção) do {@code handle} no veredito da tarefa. */
    private static <T> Result<T, String> verdict(@Nullable T value, @Nullable Throwable thrown) {
        if (thrown == null) return Result.success(value);

        val cause = thrown instanceof CompletionException && thrown.getCause() != null
                ? thrown.getCause()
                : thrown;
        Logger.error("Cache worker task failed: %s", cause.toString());
        return Result.failure(cause.toString());
    }

    private static <T> Result<T, String> rejected(RejectedExecutionException e) {
        Logger.error("Cache worker rejected task: %s", e.toString());
        return Result.failure(e.toString());
    }
}
