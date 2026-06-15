package br.community.feature.user.categories.core;

import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.MonetaryEvent;
import br.community.context.monetary._0_domain.model.MonetaryCategory;
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
    public MessageResult onCategoryCreated(MonetaryEvent.CategoryCreated event) {
        upsert(event.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCategoryUpdated(MonetaryEvent.CategoryUpdated event) {
        upsert(event.category());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onCategoryDeleted(MonetaryEvent.CategoryDeleted event) {
        delete(event.categoryId());
        return MessageResult.CONSUMED;
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(MonetaryCategory category) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", Category.from(category)));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID id) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.DELETE, Map.of("type", TYPE, "id", id.toString()));
        } catch (Exception ignored) {}
    }
}
