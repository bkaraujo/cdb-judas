package br.commons.framework.persistence.jdbc;

import br.commons.Result;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercita o modelo de transações thread-local do {@link DataSource}. O begin() devolve a transação
 * em curso da própria thread (re-obtida/continuada) mas nunca a partilha entre threads — corrida
 * que o modelo antigo introduzia ao capturar uma única instância em {@code ThreadLocal.withInitial}
 * e servi-la a todas as threads, deixando ainda o slot por limpar após o close.
 */
class DataSourceTransactionConcurrencyTest {

    private static JDBCProperties props(String dbName, int min, int max) {
        var p = new JDBCProperties();
        p.name(dbName);
        p.driver("org.h2.Driver");
        p.url("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        p.username("sa");
        p.password("");
        p.autoCommit(true);
        p.validationQuery("SELECT 1");
        p.minPoolSize(min);
        p.maxPoolSize(max);
        p.connectionTimeout(3000);
        return p;
    }

    private static JDBCTransaction unwrap(Result<JDBCTransaction, String> r) {
        return switch (r) {
            case Result.Success(var tx) -> {
                assertTrue(tx != null, "transação não devia ser nula");
                yield tx;
            }
            case Result.Failure(var err) -> throw new AssertionError("begin falhou: " + err);
        };
    }

    /**
     * Na mesma thread, begin() reentra na transação em curso; o close() é <b>balanceado</b>
     * (propagação REQUIRED): só quando o último nível fecha é que a conexão volta ao pool e o
     * begin() seguinte abre uma transação nova.
     */
    @Test
    void reentrantBeginContinuesThenFreshAfterClose() {
        var ds = new DataSource(props("txreentrant", 1, 4));
        try {
            var first = unwrap(ds.begin());
            var continued = unwrap(ds.begin());
            assertSame(first, continued, "begin() na mesma thread tem de continuar a transação em curso");

            first.close(); // fecha só o nível aninhado
            assertSame(first, unwrap(ds.begin()),
                    "close() de um nível aninhado não pode encerrar a transação em curso");

            first.close(); // fecha o nível reaberto acima
            first.close(); // fecha o nível mais externo → devolve a conexão ao pool
            assertEquals(0, ds.getActiveConnections(), "o último close tem de devolver a conexão");

            var afterClose = unwrap(ds.begin());
            assertNotSame(first, afterClose, "fechados todos os níveis, o begin() tem de abrir uma transação nova");
            afterClose.close();
        } finally {
            ds.close();
        }
    }

    /** Threads concorrentes têm de receber transações distintas (sem partilha de conexão). */
    @Test
    void concurrentBeginsGetDistinctTransactions() throws InterruptedException {
        int n = 6;
        var ds = new DataSource(props("txconcurrent", 1, n));
        try {
            var seen = ConcurrentHashMap.<JDBCTransaction>newKeySet();
            var acquired = new CountDownLatch(n);
            var hold = new CountDownLatch(1);
            var failures = new AtomicInteger(0);

            for (int i = 0; i < n; i++) {
                Thread.ofPlatform().start(() -> {
                    switch (ds.begin()) {
                        case Result.Success(var tx) -> {
                            if (tx != null) seen.add(tx);
                            acquired.countDown();
                            try { hold.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                            if (tx != null) tx.close();
                        }
                        case Result.Failure(var ignored) -> {
                            failures.incrementAndGet();
                            acquired.countDown();
                        }
                    }
                });
            }

            boolean allFast = acquired.await(2000, TimeUnit.MILLISECONDS);
            // Cada thread mantém a sua transação ativa; com partilha o Set colapsava para 1.
            Set<JDBCTransaction> snapshot = Set.copyOf(seen);
            hold.countDown();

            assertEquals(0, failures.get(), "nenhum begin devia falhar");
            assertTrue(allFast, "todas as threads deviam adquirir a sua transação depressa");
            assertEquals(n, snapshot.size(), "cada thread tem de ter a sua própria transação (partilha = corrida)");
        } finally {
            ds.close();
        }
    }

    /** Rajada de begin/close por thread: sem fugas de conexão nem starvation. */
    @Test
    void sustainedTransactionBurstReturnsConnections() throws InterruptedException {
        int threads = 12;
        int iters = 40;
        var ds = new DataSource(props("txburst", 2, 8));
        try {
            var done = new CountDownLatch(threads);
            var failures = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                Thread.ofPlatform().start(() -> {
                    for (int i = 0; i < iters; i++) {
                        switch (ds.begin()) {
                            case Result.Success(var tx) -> { if (tx != null) tx.close(); }
                            case Result.Failure(var ignored) -> failures.incrementAndGet();
                        }
                    }
                    done.countDown();
                });
            }

            assertTrue(done.await(20, TimeUnit.SECONDS), "a rajada de transações tem de terminar sem starvation");
            assertEquals(0, failures.get(), "nenhum begin devia expirar");
            assertEquals(0, ds.getActiveConnections(), "todas as conexões deviam estar devolvidas ao pool");
            assertTrue(ds.getTotalConnections() <= ds.properties().maxPoolSize(),
                    "total de conexões nunca pode exceder o máximo");
        } finally {
            ds.close();
        }
    }
}
