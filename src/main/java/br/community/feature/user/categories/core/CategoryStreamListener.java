package br.community.feature.user.categories.core;

import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.CategoryEvents;
import br.community.context.monetary._0_domain.model.Category;
import br.community.core.web.security.CurrentUser;
import br.community.feature.user.stream.SSE;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.UUID;

/** Converte eventos de domínio de categoria em mensagens SSE (tipo {@code CATEGORY}). Best-effort. */
@NullMarked
@RequiredArgsConstructor
public class CategoryStreamListener {

    private static final String TYPE = "CATEGORY";

    private final SSE sse;

    @MessageListener
    public MessageResult onCategoryCreated(CategoryEvents.Created event) {
        upsert(event.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCategoryUpdated(CategoryEvents.Updated event) {
        upsert(event.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCategoryDeleted(CategoryEvents.Deleted event) {
        delete(event.categoryId());
        return MessageResult.CONSUMED;
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(Category category) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", CategoryResponse.from(category)));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID id) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.DELETE, Map.of("type", TYPE, "id", id.toString()));
        } catch (Exception ignored) {}
    }
}
