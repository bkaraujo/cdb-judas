package br.community.feature.user.tags.core;

import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.TagEvents;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.core.web.security.CurrentUser;
import br.community.feature.user.stream.SSE;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.UUID;

/** Converte eventos de domínio de tag em mensagens SSE (tipo {@code TAG}). Payload = modelo de domínio
 *  {@link Tag}, o mesmo retornado pelo REST. Best-effort. */
@NullMarked
@RequiredArgsConstructor
public class TagStreamListener {

    private static final String TYPE = "TAG";

    private final SSE sse;

    @MessageListener
    public MessageResult onTagCreated(TagEvents.Created event) {
        upsert(event.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTagUpdated(TagEvents.Updated event) {
        upsert(event.tag());
        return MessageResult.CONSUMED;
    }

    @MessageListener
    public MessageResult onTagDeleted(TagEvents.Deleted event) {
        delete(event.tagId());
        return MessageResult.CONSUMED;
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(Tag tag) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", tag));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID id) {
        try {
            sse.dispatch(CurrentUser.getId(), SSE.Event.DELETE, Map.of("type", TYPE, "id", id.toString()));
        } catch (Exception ignored) {}
    }
}
