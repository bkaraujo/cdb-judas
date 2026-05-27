package br.community.context.shared._2_infrastructure.sse;

import br.community.context.shared._1_application.SSE;
import br.community.core.web.security.CurrentUser;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@NullMarked
public class SseService implements SSE {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe() {
        val username = CurrentUser.getUsername();
        val emitter = new SseEmitter(Long.MAX_VALUE); // Infinite timeout

        emitters
                .computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(username, emitter));
        emitter.onTimeout(() -> removeEmitter(username, emitter));
        emitter.onError((e) -> removeEmitter(username, emitter));

        // Send initial connection event
        dispatchEvent(
                emitter,
                username,
                SSE.Event.INITIALIZE,
                "Connected"
        );

        return emitter;
    }

    public void dispatch(String username, SSE.Event type, Object payload) {
        val userEmitters = emitters.get(username);
        if (userEmitters == null) return;
        userEmitters
                .forEach(emitter -> dispatchEvent(emitter, username, type, payload));
    }

    private void dispatchEvent(SseEmitter emitter, String username, SSE.Event type, Object payload) {
        try {
            emitter.send(
                    SseEmitter
                            .event()
                            .name(type.name())
                            .data(payload)
            );
        } catch (Exception e) {
            removeEmitter(username, emitter);
        }
    }

    private void removeEmitter(String username, SseEmitter emitter) {
        val userEmitters = emitters.get(username);
        if (userEmitters == null) return;

        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(username);
        }
    }
}
