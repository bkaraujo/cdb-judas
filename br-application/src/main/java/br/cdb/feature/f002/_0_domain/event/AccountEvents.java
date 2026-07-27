package br.cdb.feature.f002._0_domain.event;

import br.cdb.feature.f002._0_domain.UserAccount;
import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Vocabulário de SSE de conta: publicado pela fatia após a mutação já persistida (contexto +
 * overlay); reagido só por {@code f999} (único dono do dispatch SSE) — nenhuma fatia chama
 * {@code SSE.dispatch} direto.
 */
@NullMarked
public interface AccountEvents extends BusinessEvent {

    /** Marcador semântico — o {@code MessageBus} despacha por record concreto, não por interface. */
    @NullMarked
    interface Upsert extends AccountEvents {}

    @NullMarked
    record Created(UserAccount account) implements Upsert {}

    @NullMarked
    record Updated(UserAccount account) implements Upsert {}

    /** "conta mudou, reemita o snapshot" — publicado por f002 (cards)/f003/f004/f005/f006. */
    @NullMarked
    record Refresh(UUID accountId, String personId) implements Upsert {}

    @NullMarked
    record Deleted(UUID accountId, String personId) implements AccountEvents {}
}
