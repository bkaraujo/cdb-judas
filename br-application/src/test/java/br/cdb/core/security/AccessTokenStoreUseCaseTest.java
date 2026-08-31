package br.cdb.core.security;

import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccessTokenStoreUseCaseTest {

    private AccessTokenStore store;
    private List<SessionEvents.Logout> logouts;

    @BeforeEach
    void setUp() {
        store = new AccessTokenStore();
        logouts = new ArrayList<>();
        MessageBus.reset();
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onLogout(SessionEvents.Logout e) {
        logouts.add(e);
        return MessageResult.CONSUMED;
    }

    @Test
    void testRotateDoesNotEmitLogout() {
        val personId = UUID.randomUUID().toString();
        val session = store.open(UUID.randomUUID().toString(), personId, "user");
        val token = session.token();

        store.rotate(token);
        assertTrue(logouts.isEmpty(), "rotate() should not emit Logout");

        store.rotate(token);
        assertTrue(logouts.isEmpty(), "rotate() twice should not emit Logout");
    }

    @Test
    void testConsumeEphemeralInvalidDoesNotEmitLogout() {
        val personId = UUID.randomUUID().toString();
        store.open(UUID.randomUUID().toString(), personId, "user");

        val result = store.consumeEphemeral("fake-token");
        assertTrue(result.isEmpty());
        assertTrue(logouts.isEmpty());
    }

    @Test
    void testRotateMultipleTimesDoesNotEmitLogout() {
        val personId = UUID.randomUUID().toString();
        var session = store.open(UUID.randomUUID().toString(), personId, "user");

        for (int i = 0; i < 5; i++) {
            val result = store.rotate(session.token());
            assertTrue(result.isPresent());
            session = result.get();
        }

        assertTrue(logouts.isEmpty(), "Multiple rotations should not emit Logout");
    }
}
