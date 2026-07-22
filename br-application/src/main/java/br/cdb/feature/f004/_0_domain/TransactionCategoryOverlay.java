package br.cdb.feature.f004._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Porta que a fatia de categorias ({@code f004}) exige do overlay de transações ({@code
 * PERSON_TRANSACTION}) para apagar/reatribuir uma subárvore de categorias — precisa contar/localizar
 * as transações vinculadas (retorno síncrono) e re-keyar a categoria no overlay antes de apagar a
 * subárvore. Implementada pela fatia dona do overlay (f005) — inversão de dependência: f005 depende
 * de f004, nunca o contrário.
 */
@NullMarked
public interface TransactionCategoryOverlay {

    /** IDs das transações da pessoa vinculadas a qualquer categoria do conjunto. */
    List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds);

    /** Re-keya o overlay das transações da categoria de origem para a de destino (estratégia MOVE). */
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId);
}
