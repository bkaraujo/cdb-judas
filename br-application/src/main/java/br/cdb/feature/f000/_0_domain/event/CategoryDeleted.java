package br.cdb.feature.f000._0_domain.event;

import br.commons.business.BusinessEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Publicado por {@code f005.WriteUseCase.deleteCategory} após validar a subárvore a apagar.
 * Reagido por {@code CategoryDeletedListener} (f005), que apaga as linhas {@code PERSON_CATEGORY}
 * da subárvore. Mora em {@code f000} (não em quem publica) para respeitar a ordem de fatias.
 */
@NullMarked
public record CategoryDeleted(List<UUID> categoryIds, UUID personId) implements BusinessEvent {}
