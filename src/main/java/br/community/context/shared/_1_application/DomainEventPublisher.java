package br.community.context.shared._1_application;

import br.commons.MessageBus;
import br.community.context.shared._0_domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class DomainEventPublisher {
    private DomainEventPublisher(){}

    public static void upsert(String entityType, Object payload) {
        MessageBus.submit(new DomainEvent.EntityUpserted(entityType, payload));
    }

    public static void delete(String entityType, String entityId) {
        MessageBus.submit(new DomainEvent.EntityDeleted(entityType, entityId));
    }
}
