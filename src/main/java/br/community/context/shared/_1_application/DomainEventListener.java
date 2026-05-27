package br.community.context.shared._1_application;

import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.shared._0_domain.event.DomainEvent;
import br.community.core.web.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
@RequiredArgsConstructor
public class DomainEventListener {

    private final SSE sse;

    @MessageListener
    @SuppressWarnings("EmptyCatch")
    public MessageResult onEntityUpserted(DomainEvent.EntityUpserted event) {
        try {
            sse.dispatch(
                    CurrentUser.getUsername(),
                    SSE.Event.UPSERT,
                    Map.of("type", event.entityType(), "payload", event.payload())
            );
        } catch (Exception ignored) {}
        return MessageResult.CONSUMED;
    }

    @MessageListener
    @SuppressWarnings("EmptyCatch")
    public MessageResult onEntityDeleted(DomainEvent.EntityDeleted event) {
        try {
            sse.dispatch(
                    CurrentUser.getUsername(),
                    SSE.Event.DELETE,
                    Map.of("type", event.entityType(), "id", event.entityId())
            );
        } catch (Exception ignored) {}
        return MessageResult.CONSUMED;
    }
}
