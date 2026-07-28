package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Vocabulário de SSE de conta: publicado pela fatia após a mutação já persistida (contexto +
 * overlay); reagido só por {@code f999} (único dono do dispatch SSE) — nenhuma fatia chama
 * {@code SSE.dispatch} direto. Mora em {@code f000} (não em f002, que a publica) para que qualquer
 * fatia de negócio possa publicá-lo sem depender de f002 — ver regra
 * {@code feature_slices_must_not_depend_on_sibling_slices}.
 */
@NullMarked
public interface AccountStreamEvents extends BusinessEvent {

    /** Marcador semântico — o {@code MessageBus} despacha por record concreto, não por interface. */
    @NullMarked
    interface Upsert extends AccountStreamEvents {}

    @NullMarked
    record Created(UUID accountId, String personId) implements Upsert {}

    @NullMarked
    record Updated(UUID accountId, String personId) implements Upsert {}

    /** "conta mudou, reemita o snapshot" — publicado por f002 (cards)/f004/f005/f006/f007. */
    @NullMarked
    record Refresh(UUID accountId, String personId) implements Upsert {}

    @NullMarked
    record Deleted(UUID accountId, String personId) implements AccountStreamEvents {}
}
