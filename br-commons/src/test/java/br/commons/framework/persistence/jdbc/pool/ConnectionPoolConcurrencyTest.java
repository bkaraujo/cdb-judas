package br.commons.framework.persistence.jdbc.pool;

import br.commons.Result;
import br.commons.framework.persistence.jdbc.JDBCProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduz a exaustão do pool sob concorrência (sintoma: "Connection timeout: no connection
 * available within ...ms"). O caminho da aplicação devolve sempre as conexões; o defeito está no
 * próprio pool quando vários pedidos concorrem — cenário nunca exercido pelos testes MockMvc
 * (sequenciais) nem pelos repositórios em memória.
 */
class ConnectionPoolConcurrencyTest {

    private static JDBCProperties props(String dbName, int min, int max, int timeoutMs) {
        var p = new JDBCProperties();
        p.driver("org.h2.Driver");
        p.url("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        p.username("sa");
        p.password("");
        p.autoCommit(true);
        p.validationQuery("SELECT 1");
        p.minPoolSize(min);
        p.maxPoolSize(max);
        p.connectionTimeout(timeoutMs);
        return p;
    }

    /**
     * Com N pedidos concorrentes (N &le; max) o pool tem de crescer e atendê-los todos depressa.
     * O pool com bug bloqueia cada esperante pelo connectionTimeout inteiro antes de criar uma nova
     * conexão, logo N-1 deles ficam parados ~timeout em vez de adquirir de imediato.
     */
    @Test
    void growsToMeetConcurrentDemand() throws InterruptedException {
        var pool = new ConnectionPool(props("poolgrow", 1, 6, 3000));
        try {
            int n = 6;
            var acquired = new CountDownLatch(n);
            var hold = new CountDownLatch(1);
            var failures = new AtomicInteger(0);

            for (int i = 0; i < n; i++) {
                Thread.ofPlatform().start(() -> {
                    switch (pool.aquire()) {
                        case Result.Success(var conn) -> {
                            acquired.countDown();
                            try { hold.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                            conn.close();
                        }
                        case Result.Failure(var ignored) -> {
                            failures.incrementAndGet();
                            acquired.countDown();
                        }
                    }
                });
            }

            boolean allFast = acquired.await(1000, TimeUnit.MILLISECONDS);
            hold.countDown();

            assertEquals(0, failures.get(), "nenhum pedido devia falhar ao adquirir");
            assertTrue(allFast, "pool tem de crescer para servir " + n + " pedidos concorrentes depressa (cresceu tarde demais?)");
        } finally {
            pool.close();
        }
    }

    /**
     * Rajada sustentada não pode perder conexões nem exceder o máximo: ao fim de todo o trabalho a
     * contabilidade tem de estar consistente (sem offer() silenciosamente descartado, sem total
     * acima do máximo por corrida no check-then-create).
     */
    @Test
    void sustainedBurstKeepsAccountingConsistent() throws InterruptedException {
        var pool = new ConnectionPool(props("poolburst", 2, 8, 3000));
        try {
            int threads = 16;
            int iters = 60;
            var done = new CountDownLatch(threads);
            var failures = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                Thread.ofPlatform().start(() -> {
                    for (int i = 0; i < iters; i++) {
                        switch (pool.aquire()) {
                            case Result.Success(var conn) -> conn.close();
                            case Result.Failure(var ignored) -> failures.incrementAndGet();
                        }
                    }
                    done.countDown();
                });
            }

            assertTrue(done.await(20, TimeUnit.SECONDS), "a rajada tem de terminar sem starvation");
            assertEquals(0, failures.get(), "nenhuma aquisição devia expirar");
            assertTrue(pool.totalCount() <= pool.properties().maxPoolSize(), "total de conexões nunca pode exceder o máximo (corrida no create?)");
            assertEquals(0, pool.activeCount(), "todas as conexões deviam estar devolvidas");
            assertEquals(pool.totalCount(), pool.availableCount(), "conexões devolvidas têm de voltar ao pool (offer descartado em silêncio?)");
        } finally {
            pool.close();
        }
    }
}
