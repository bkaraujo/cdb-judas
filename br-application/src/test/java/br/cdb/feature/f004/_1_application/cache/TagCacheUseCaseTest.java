package br.cdb.feature.f004._1_application.cache;

import br.cdb.feature.f004._0_domain.model.Tag;
import br.commons.platform.NativeCache;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagCacheUseCaseTest {

    @Test
    void testRoundTrip() {
        try (val cache = new NativeCache<String>()) {
            val seg = cache.put("test", TagLayout.SIZE).get();

            val id = UUID.randomUUID();
            val personId = UUID.randomUUID();
            val createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
            val tag = new Tag(id, personId, "My Tag", "#FF5733", createdAt);

            TagLayout.write(seg, tag);

            val view = new TagLayout.View().bind(seg);
            assertEquals(id, view.id());
            assertEquals(personId, view.personId());
            assertEquals("My Tag", view.name());
            assertEquals("#FF5733", view.color());
            assertNotNull(view.createdAt());
        }
    }

    @Test
    void testNullCreatedAt() {
        try (val cache = new NativeCache<String>()) {
            val seg = cache.put("test", TagLayout.SIZE).get();

            val id = UUID.randomUUID();
            val personId = UUID.randomUUID();
            val tag = new Tag(id, personId, "Tag", "#000000", null);

            TagLayout.write(seg, tag);

            val view = new TagLayout.View().bind(seg);
            assertNull(view.createdAt());
        }
    }

    @Test
    void testZeroAllocAccess() {
        try (val cache = new NativeCache<String>()) {
            val seg = cache.put("test", TagLayout.SIZE).get();
            val id = UUID.randomUUID();

            TagLayout.write(seg, new Tag(id, UUID.randomUUID(), "test", "#000", null));

            val view = new TagLayout.View().bind(seg);
            long msb = view.idMsb();
            long lsb = view.idLsb();

            assertEquals(id.getMostSignificantBits(), msb);
            assertEquals(id.getLeastSignificantBits(), lsb);
        }
    }
}
