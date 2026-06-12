package br.community.context.security;

import br.commons.framework.persistence.inmemory.InMemoryStorage;
import br.community.context.security._0_domain.model.Preferences;
import br.community.context.security._0_domain.model.User;
import br.community.context.security._2_infrastructure.UserJsonRepository;
import br.community.core.web.security.AccessTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserRegistryTest {

    private final InMemoryStorage storage = new InMemoryStorage();
    private final UserJsonRepository repository = new UserJsonRepository(storage, new ObjectMapper());

    @Test
    void resolvePorUsernameEPorId() {
        var id = UUID.randomUUID().toString();
        repository.save(new User(id, "admin", null, "hash", Preferences.defaults()));

        assertTrue(repository.findByUsername("admin").isPresent());
        assertEquals(id, repository.findByUsername("admin").orElseThrow().id());
        assertTrue(repository.findById(id).isPresent());
        assertEquals("admin", repository.findById(id).orElseThrow().username());

        assertTrue(repository.findByUsername("fantasma").isEmpty());
        assertTrue(repository.findById("inexistente").isEmpty());
    }

    @Test
    void saveFazUpsertPorId() {
        var id = UUID.randomUUID().toString();
        repository.save(new User(id, "admin", null, "hash1", Preferences.defaults()));
        repository.save(new User(id, "admin", null, "hash2", Preferences.defaults()));

        assertEquals(1, repository.findByUsername("admin").stream().count());
        assertEquals("hash2", repository.findById(id).orElseThrow().password());
    }

    @Test
    void tokenEmiteRotacionaEMapeiaParaId() {
        var id = UUID.randomUUID().toString();
        repository.save(new User(id, "admin", null, "hash", Preferences.defaults()));

        var store = new AccessTokenStore();
        var token = store.issue(id);
        assertEquals(id, store.validate(token).orElseThrow());

        var rotation = store.rotate(token).orElseThrow();
        assertEquals(id, rotation.userId());
        assertNotEquals(token, rotation.nextToken());
        assertTrue(store.validate(token).isEmpty(), "token antigo invalidado após rotação");
        assertEquals(id, store.validate(rotation.nextToken()).orElseThrow());

        // username permanece resolvível a partir do id (para logs)
        assertEquals("admin", repository.findById(rotation.userId()).orElseThrow().username());
    }
}
