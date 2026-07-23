package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Publicado por {@code AccountUseCase.deleteAccount} (f002) após o contexto monetário apagar a
 * conta. Reagido por {@code AccountDeletedListener} (f002), que purga o overlay
 * {@code PERSON_ACCOUNT} da própria pessoa/conta. Mora em {@code f000} (não em quem publica) para
 * respeitar a ordem de fatias.
 */
@NullMarked
public record AccountDeleted(UUID accountId, UUID personId) implements BusinessEvent {}
