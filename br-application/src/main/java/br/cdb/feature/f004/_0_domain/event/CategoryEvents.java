package br.cdb.feature.f004._0_domain.event;

import br.cdb.feature.f004._0_domain.UserCategory;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Vocabulário de SSE de categoria — reagido só por {@code f999} (único dono do dispatch SSE). */
@NullMarked
public interface CategoryEvents extends BusinessEvent {

    @NullMarked
    record Created(UserCategory category) implements CategoryEvents {}

    @NullMarked
    record Updated(UserCategory category) implements CategoryEvents {}

    @NullMarked
    record Deleted(UUID categoryId, UUID personId) implements CategoryEvents {}
}
