package br.cdb.feature.f004._0_domain.event;

import br.cdb.feature.f004._0_domain.UserTag;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Vocabulário de SSE de tag — reagido só por {@code f999} (único dono do dispatch SSE). */
@NullMarked
public interface TagEvents extends BusinessEvent {

    @NullMarked
    record Created(UserTag tag) implements TagEvents {}

    @NullMarked
    record Updated(UserTag tag) implements TagEvents {}

    @NullMarked
    record Deleted(UUID tagId, UUID personId) implements TagEvents {}
}
