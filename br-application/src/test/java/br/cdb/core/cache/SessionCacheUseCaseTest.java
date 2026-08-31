package br.cdb.core.cache;

import br.commons.platform.NativeCache;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da infra de cache off-heap por sessão.
 */
@NullMarked
class SessionCacheUseCaseTest {

    @Test
    @DisplayName("popula cache no login e limpa no logout")
    void populatesOnLoginClearsOnLogout() {
        val cache = new SessionScopedCache<String>(
                "TEST:",
                personId -> List.of("item1", "item2", "item3"),
                s -> UUID.nameUUIDFromBytes(s.getBytes()),
                s -> 100,
                (seg, model) -> NativeCache.writeString(seg, 0, 100, model)
        );

        val populate = cache.onLogin("person-1");
        assertTrue(CacheWorker.waitForPending().isSuccess(), "worker deveria drenar as tarefas");
        assertTrue(populate.join().isSuccess(), "populate deveria terminar sem erro");

        val items = new java.util.ArrayList<String>();
        assertTrue(cache.forEach("person-1", seg -> items.add(NativeCache.readString(seg, 0))).isSuccess());

        assertTrue(items.size() == 3, "cache deveria ter 3 items");

        assertTrue(cache.onLogout("person-1").join().isSuccess());
        val itemsAfter = new java.util.ArrayList<String>();
        val afterLogout = cache.forEach("person-1", seg -> itemsAfter.add(NativeCache.readString(seg, 0)));

        assertTrue(afterLogout.isSuccess(), "sem sessão é miss (sucesso vazio), não falha");
        assertTrue(itemsAfter.isEmpty(), "cache deveria estar vazio após logout");
    }

    @Test
    @Disabled("Medição de zero-alloc requer ThreadMXBean.getCurrentThreadAllocatedBytes(), " +
              "indisponível em ambiente de teste. Para medir: rodar SessionScopedCacheTest.zeroAllocFlyweight() " +
              "em app real com JDK 25 via jdk.management e coletar alocações via ThreadMXBean. " +
              "Esperado: < 1 KiB para 1M iterações de view.bind(seg); view.idMsb(); view.idLsb().")
    @DisplayName("zero-alloc do flyweight: 1M bind+read < 1KiB")
    void zeroAllocFlyweight() {
        // Skeleton: implementar via ThreadMXBean quando disponível em ambiente de produção
        // val threadMx = ManagementFactory.getThreadMXBean();
        // long before = threadMx.getCurrentThreadAllocatedBytes();
        // for (int i = 0; i < 1_000_000; i++) { view.bind(seg); view.idMsb(); view.idLsb(); }
        // long after = threadMx.getCurrentThreadAllocatedBytes();
        // assertTrue((after - before) < 1024, "alocou " + (after - before) + " bytes");
    }
}
