package br.cdb.core.cache;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@NullMarked
public final class CacheWorker {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(
            r -> { val t = new Thread(r, "cache-worker"); t.setDaemon(true); return t; });

    private CacheWorker() {}

    /** Nunca deixa exceção escapar: o publicador do evento está dentro da transação dele. */
    public static void submit(Runnable task) {
        WORKER.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(CacheWorker.class.getName())
                    .severe("Cache worker task failed: " + e.getMessage());
            }
        });
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(CacheWorker.class.getName())
                    .severe("Cache worker task failed: " + e.getMessage());
                return null;
            }
        }, WORKER);
    }

    public static void awaitIdle() throws InterruptedException {
        WORKER.shutdown();
        if (!WORKER.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
            WORKER.shutdownNow();
        }
    }

    /** Espera todas as tarefas pending terminarem SEM desligar o worker (para testes). */
    public static void waitForPending() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        // Submete uma tarefa marcadora que retorna quando executada. Quando obtiver o resultado,
        // todas as tarefas anteriores já completaram (FIFO do executor).
        CompletableFuture<Void> marker = new CompletableFuture<>();
        submit(() -> marker.complete(null));
        marker.get(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}
