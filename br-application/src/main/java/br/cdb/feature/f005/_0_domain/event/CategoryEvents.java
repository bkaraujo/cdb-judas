package br.cdb.feature.f005._0_domain.event;

import br.cdb.feature.f005._0_domain.model.Category;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/** Vocabulário de SSE de categoria — reagido só por {@code f999} (único dono do dispatch SSE). */
@NullMarked
public interface CategoryEvents extends BusinessEvent {

    @NullMarked
    record Created(Category category) implements CategoryEvents {}

    @NullMarked
    record Updated(Category category) implements CategoryEvents {}

    @NullMarked
    record Deleted(List<UUID> categoryIds, UUID personId) implements CategoryEvents {}

}
