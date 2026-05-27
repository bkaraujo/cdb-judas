package br.community.context.shared._0_domain.event;

import br.commons.framework.message.Message;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface DomainEvent extends Message {

    @NullMarked
    record EntityUpserted(String entityType, Object payload) implements DomainEvent {
        public String toString() {
            return "EntityUpserted(" + entityType + ")";
        }
    }

    @NullMarked
    record EntityDeleted(String entityType, String entityId) implements DomainEvent {
        public String toString() {
            return "EntityDeleted(" + entityType + ")";
        }
    }

}
