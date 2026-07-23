package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Publicado por {@code TagUseCase.deleteTag} (f003) após reatribuir (MOVE) ou apagar as
 * transações vinculadas (DELETE). Reagido por {@code TagDeletedListener} (f003), que purga
 * {@code PERSON_TRANSACTION_TAG} e a linha {@code PERSON_TAG} — idempotente: no MOVE os vínculos já
 * foram reatribuídos antes da publicação, então a purga é no-op. Mora em {@code f000} (não em quem
 * publica) para respeitar a ordem de fatias.
 */
@NullMarked
public record TagDeleted(UUID tagId, UUID personId) implements BusinessEvent {}
