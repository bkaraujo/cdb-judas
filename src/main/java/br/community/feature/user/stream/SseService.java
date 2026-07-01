package br.community.feature.user.stream;

import br.community.core.web.security.CurrentUser;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@NullMarked
public class SseService implements SSE {

    @NullMarked
    private record Channel(SseBroadcaster broadcaster, AtomicInteger connections) {}

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    /**
     * {@code Sse} é uma factory sem estado (o RESTEasy Reactive expõe sempre a mesma instância
     * subjacente via {@code @Context}); cacheada aqui na primeira subscription para permitir
     * {@link #dispatch} fora do escopo de uma request (chamado pelo {@code MessageBus}).
     */
    private volatile @Nullable Sse sse;

    @Override
    public void subscribe(SseEventSink sink, Sse requestSse) {
        this.sse = requestSse;
        val userId = CurrentUser.getId();
        val channel = channels.computeIfAbsent(userId, ignored -> newChannel(userId, requestSse));
        channel.connections().incrementAndGet();
        channel.broadcaster().register(sink);

        sink.send(event(requestSse, SSE.Event.INITIALIZE, "Connected"));
    }

    @Override
    public void dispatch(String username, SSE.Event type, Object payload) {
        val currentSse = sse;
        val channel = channels.get(username);
        if (currentSse == null || channel == null) return;
        channel.broadcaster().broadcast(event(currentSse, type, payload));
    }

    private Channel newChannel(String userId, Sse requestSse) {
        val broadcaster = requestSse.newBroadcaster();
        val connections = new AtomicInteger(0);
        // Só decrementa no close (não no error) para nunca remover o canal cedo demais
        // enquanto outra aba do mesmo usuário ainda pode estar conectada.
        broadcaster.onClose(ignored -> {
            if (connections.decrementAndGet() <= 0) {
                channels.remove(userId);
            }
        });
        return new Channel(broadcaster, connections);
    }

    private static OutboundSseEvent event(Sse sse, SSE.Event type, Object payload) {
        return sse.newEventBuilder()
                .name(type.name())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(payload)
                .build();
    }
}
