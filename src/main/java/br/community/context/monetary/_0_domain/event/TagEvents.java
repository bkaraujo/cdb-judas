package br.community.context.monetary._0_domain.event;

import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.shared._0_domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface TagEvents extends DomainEvent {

    @NullMarked
    record Created(Tag tag) implements TagEvents {}

    @NullMarked
    record Updated(Tag tag) implements TagEvents {}

    @NullMarked
    record Deleted(UUID tagId) implements TagEvents {}
}
