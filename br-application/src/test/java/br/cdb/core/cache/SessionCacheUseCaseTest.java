package br.cdb.core.cache;

import br.commons.platform.NativeCache;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SessionCacheUseCaseTest {

    static class Model {
        UUID id;
        String name;

        Model(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private SessionScopedCache<Model> cache;
    private List<Model> fakeDb;
    private AtomicInteger loadCount;

    @BeforeEach
    void setUp() {
        fakeDb = new ArrayList<>();
        loadCount = new AtomicInteger(0);

        cache = new SessionScopedCache<>(
            "MODEL:",
            personId -> {
                loadCount.incrementAndGet();
                return new ArrayList<>(fakeDb);
            },
            m -> m.id,
            m -> 64,
            (seg, m) -> {
                NativeCache.writeUuid(seg, 0, m.id);
                NativeCache.writeString(seg, 0 + 16, 48, m.name);
            }
        );
    }

    @Test
    void testLoginPopulates() throws InterruptedException {
        val id1 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));

        cache.onLogin("p1");
        Thread.sleep(100);

        assertEquals(1, loadCount.get());
    }

    @Test
    void testSecondLoginCreatesNewCache() throws InterruptedException {
        val id1 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));

        cache.onLogin("p1");
        Thread.sleep(100);
        int count1 = loadCount.get();

        val id2 = UUID.randomUUID();
        fakeDb.add(new Model(id2, "model2"));

        cache.onLogin("p1");
        Thread.sleep(100);
        int count2 = loadCount.get();

        assertEquals(2, count2);
    }

    @Test
    void testLogoutCloses() throws InterruptedException {
        val id1 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));

        cache.onLogin("p1");
        Thread.sleep(100);

        cache.onLogout("p1");
        Thread.sleep(100);

        AtomicInteger found = new AtomicInteger(0);
        cache.forEach("p1", seg -> found.incrementAndGet());
        assertEquals(0, found.get());
    }

    @Test
    void testForEachBeforeLoginIsNoop() {
        AtomicInteger count = new AtomicInteger(0);
        cache.forEach("p1", seg -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    void testForEachAfterPopulate() throws InterruptedException {
        val id1 = UUID.randomUUID();
        val id2 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));
        fakeDb.add(new Model(id2, "model2"));

        cache.onLogin("p1");
        Thread.sleep(100);

        AtomicInteger count = new AtomicInteger(0);
        cache.forEach("p1", seg -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    void testFind() throws InterruptedException {
        val id1 = UUID.randomUUID();
        val id2 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));
        fakeDb.add(new Model(id2, "model2"));

        cache.onLogin("p1");
        Thread.sleep(100);

        assertTrue(cache.find("p1", id1, seg -> {
            val readId = NativeCache.readUuid(seg, 0);
            assertEquals(id1, readId);
        }));

        assertFalse(cache.find("p1", UUID.randomUUID(), seg -> {}));
    }

    @Test
    void testUpsert() throws InterruptedException {
        val id1 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));

        cache.onLogin("p1");
        Thread.sleep(100);

        val id2 = UUID.randomUUID();
        cache.upsert("p1", new Model(id2, "model2"));
        Thread.sleep(100);

        AtomicInteger count = new AtomicInteger(0);
        cache.forEach("p1", seg -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    void testEvict() throws InterruptedException {
        val id1 = UUID.randomUUID();
        val id2 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));
        fakeDb.add(new Model(id2, "model2"));

        cache.onLogin("p1");
        Thread.sleep(100);

        cache.evict("p1", id1);
        Thread.sleep(100);

        AtomicInteger count = new AtomicInteger(0);
        cache.forEach("p1", seg -> count.incrementAndGet());
        assertEquals(1, count.get());

        assertTrue(cache.find("p1", id2, seg -> {}));
        assertFalse(cache.find("p1", id1, seg -> {}));
    }

    @Test
    void testEvictEverywhere() throws InterruptedException {
        val id1 = UUID.randomUUID();
        val id2 = UUID.randomUUID();
        fakeDb.add(new Model(id1, "model1"));
        fakeDb.add(new Model(id2, "model2"));

        cache.onLogin("p1");
        cache.onLogin("p2");
        Thread.sleep(100);

        cache.evictEverywhere(id1);
        Thread.sleep(100);

        AtomicInteger count1 = new AtomicInteger(0);
        cache.forEach("p1", seg -> count1.incrementAndGet());
        assertEquals(1, count1.get());

        AtomicInteger count2 = new AtomicInteger(0);
        cache.forEach("p2", seg -> count2.incrementAndGet());
        assertEquals(1, count2.get());
    }
}
