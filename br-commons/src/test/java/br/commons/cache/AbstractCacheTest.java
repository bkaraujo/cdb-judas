package br.commons.cache;

import br.commons.Result;
import br.commons.platform.NativeCache;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCacheTest {

    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    private record Item(UUID id, long amount) {}

    /** Layout mínimo: uuid (16) + amount (8). */
    private static final long SIZE = 24;

    private static final class ItemCache extends AbstractCache<Item> {
        ItemCache(Map<String, List<Item>> source) {
            super("TEST:",
                    personId -> source.getOrDefault(personId, List.of()),
                    Item::id,
                    _ -> SIZE,
                    (segment, item) -> {
                        NativeCache.writeUuid(segment, 0, item.id());
                        segment.set(ValueLayout.JAVA_LONG_UNALIGNED, 16, item.amount());
                    });
        }

        @Override
        protected Item mapToDomain(MemorySegment segment) {
            return new Item(
                    Objects.requireNonNull(NativeCache.readUuid(segment, 0)),
                    segment.get(ValueLayout.JAVA_LONG_UNALIGNED, 16));
        }
    }

    private static ItemCache loggedIn(String personId, List<Item> items) {
        val cache = new ItemCache(Map.of(personId, items));
        assertTrue(cache.onLogin(personId).join().isSuccess());
        return cache;
    }

    private static Item item(long amount) {
        return new Item(UUID.randomUUID(), amount);
    }

    @Test
    void testFindHit() {
        val one = item(10);
        val two = item(20);
        val cache = loggedIn(ALICE, List.of(one, two));

        assertEquals(two, cache.find(ALICE, two.id()).get());
        assertEquals(one, cache.find(ALICE, one.id()).get());

        cache.onLogout(ALICE).join();
    }

    @Test
    void testFindMissKeepsPrefixInMessage() {
        val stored = item(10);
        val cache = loggedIn(ALICE, List.of(stored));
        val absent = UUID.randomUUID();

        assertEquals("not found: TEST:" + absent, failureOf(cache.find(ALICE, absent)));

        cache.onLogout(ALICE).join();
    }

    @Test
    void testFindWithoutLoginIsMiss() {
        val cache = new ItemCache(Map.of());
        val id = UUID.randomUUID();

        assertEquals("not found: TEST:" + id, failureOf(cache.find(ALICE, id)));
    }

    @Test
    void testListKeepsLoadOrderAndIsMutable() {
        val one = item(10);
        val two = item(20);
        val three = item(30);
        val cache = loggedIn(ALICE, List.of(one, two, three));

        val listed = cache.list(ALICE).get();
        assertEquals(List.of(one, two, three), listed);

        // A lista é do chamador: quem lista costuma ordenar/filtrar em cima dela.
        listed.remove(0);
        assertEquals(3, cache.list(ALICE).get().size());

        cache.onLogout(ALICE).join();
    }

    @Test
    void testListWithoutLoginIsEmpty() {
        val cache = new ItemCache(Map.of());

        val listed = cache.list(ALICE);
        assertTrue(listed.isSuccess());
        assertTrue(listed.get().isEmpty());
    }

    @Test
    void testUpsertAndEvict() {
        val stored = item(10);
        val cache = loggedIn(ALICE, List.of(stored));

        val added = item(99);
        assertTrue(cache.upsert(ALICE, added).join().isSuccess());
        assertEquals(added, cache.find(ALICE, added.id()).get());
        assertEquals(2, cache.list(ALICE).get().size());

        val updated = new Item(stored.id(), 11);
        assertTrue(cache.upsert(ALICE, updated).join().isSuccess());
        assertEquals(updated, cache.find(ALICE, stored.id()).get());
        assertEquals(2, cache.list(ALICE).get().size());

        assertTrue(cache.evict(ALICE, stored.id()).join().isSuccess());
        assertTrue(cache.find(ALICE, stored.id()).isFailure());
        assertEquals(List.of(added), cache.list(ALICE).get());

        cache.onLogout(ALICE).join();
    }

    @Test
    void testEvictEverywhereHitsEverySession() {
        val shared = item(10);
        val cache = new ItemCache(Map.of(ALICE, List.of(shared), BOB, List.of(shared)));
        assertTrue(cache.onLogin(ALICE).join().isSuccess());
        assertTrue(cache.onLogin(BOB).join().isSuccess());

        assertTrue(cache.evictEverywhere(shared.id()).join().isSuccess());

        assertTrue(cache.find(ALICE, shared.id()).isFailure());
        assertTrue(cache.find(BOB, shared.id()).isFailure());

        cache.onLogout(ALICE).join();
        cache.onLogout(BOB).join();
    }

    @Test
    void testLogoutDropsTheSession() {
        val stored = item(10);
        val cache = loggedIn(ALICE, List.of(stored));

        assertTrue(cache.onLogout(ALICE).join().isSuccess());

        assertTrue(cache.find(ALICE, stored.id()).isFailure());
        assertTrue(cache.list(ALICE).get().isEmpty());

        // Segundo logout não tem sessão para fechar, e não é erro.
        assertTrue(cache.onLogout(ALICE).join().isSuccess());
    }

    @Test
    void testSessionsDoNotSeeEachOther() {
        val mine = item(10);
        val yours = item(20);
        val cache = new ItemCache(Map.of(ALICE, List.of(mine), BOB, List.of(yours)));
        assertTrue(cache.onLogin(ALICE).join().isSuccess());
        assertTrue(cache.onLogin(BOB).join().isSuccess());

        assertEquals(List.of(mine), cache.list(ALICE).get());
        assertEquals(List.of(yours), cache.list(BOB).get());
        assertTrue(cache.find(ALICE, yours.id()).isFailure());

        cache.onLogout(ALICE).join();
        cache.onLogout(BOB).join();
    }

    private static <T> String failureOf(Result<T, String> result) {
        if (result instanceof Result.Failure<T, String>(var error)) return error;
        throw new AssertionError("esperava falha, veio " + result);
    }
}
