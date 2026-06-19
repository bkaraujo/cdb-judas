package br.community.context.monetary._0_domain.event;

import br.community.context.monetary._0_domain.model.Category;
import br.community.context.shared._0_domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface CategoryEvents extends DomainEvent {

    @NullMarked
    record Created(Category category) implements CategoryEvents {}

    @NullMarked
    record Updated(Category category) implements CategoryEvents {}

    @NullMarked
    record Deleted(UUID categoryId) implements CategoryEvents {}
}
