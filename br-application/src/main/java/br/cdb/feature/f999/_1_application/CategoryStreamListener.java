package br.cdb.feature.f999._1_application;

import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f004._0_domain.UserCategory;
import br.cdb.feature.f004._0_domain.event.CategoryEvents;
import br.cdb.feature.f004._1_application.CategoryResponse;
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

/** Único dono do dispatch SSE de categoria — reage ao vocabulário {@link CategoryEvents}. */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class CategoryStreamListener {

    private static final String TYPE = "CATEGORY";

    private final SSE sse;

    void subscribe(@Observes StartupEvent event) {
        MessageBus.subscribe(this);
    }

    @MessageListener
    public MessageResult onCreated(CategoryEvents.Created event) {
        dispatchUpsert(event.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onUpdated(CategoryEvents.Updated event) {
        dispatchUpsert(event.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onDeleted(CategoryEvents.Deleted event) {
        dispatchDelete(event.personId(), event.categoryId());
        return MessageResult.CONSUMED;
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchUpsert(UserCategory category) {
        try {
            sse.dispatch(category.personId().toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", CategoryResponse.from(category)));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void dispatchDelete(UUID personId, UUID categoryId) {
        try {
            sse.dispatch(personId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", categoryId.toString()));
        } catch (Exception ignored) {}
    }
}
