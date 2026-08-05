package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Publicado por {@code f005.WriteUseCase.moveCategorySubtree} antes de {@link CategoryDeleted}
 * — a subárvore antiga precisa estar re-keyada no vínculo antes de sumir. Reagido por
 * {@code TransactionOverlayListener} (f006), que re-keya {@code F006_TRANSACTION_CATEGORY} para
 * {@code newCategoryId}. Mora em {@code f000} (não em quem publica) para respeitar a ordem de fatias.
 */
@NullMarked
public record CategoryReassigned(List<UUID> oldCategoryIds, UUID newCategoryId, UUID personId) implements BusinessEvent {}
