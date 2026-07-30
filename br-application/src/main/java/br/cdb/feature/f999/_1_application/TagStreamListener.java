package br.cdb.feature.f999._1_application;

import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.commons.MessageBus;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.UUID;

/** Único dono do dispatch SSE de tag — reage ao vocabulário {@link TagEvents}; payload cru (sem DTO própria). */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagStreamListener {

    private static final String TYPE = "TAG";

    private final SSE sse;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onCreated(TagEvents.Created event) {
        dispatchUpsert(event.tag().personId(), event.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(TagEvents.Updated event) {
        dispatchUpsert(event.tag().personId(), event.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(TagEvents.Deleted event) {
        dispatchDelete(event.tag().personId(), event.tag().id());
        return MessageResult.CONSUMED;
    }

    /** Payload cru: a tag entra como {@code Object} para não importar o modelo de f004 (só o vocabulário de evento). */
    @SuppressWarnings("EmptyCatch")
    private void dispatchUpsert(UUID personId, Object tag) {
        try {
            sse.dispatch(personId.toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", tag));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchDelete(UUID personId, UUID tagId) {
        try {
            sse.dispatch(personId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", tagId.toString()));
        } catch (Exception ignored) {}
    }
}
