package br.cdb.feature.f005._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Porta que a fatia de categorias ({@code f005}) exige do overlay de transações ({@code
 * PERSON_TRANSACTION}) para apagar/reatribuir uma subárvore de categorias — precisa contar/localizar
 * as transações vinculadas (retorno síncrono) e re-keyar a categoria no overlay antes de apagar a
 * subárvore. Implementada por um adapter em {@code f999._2_infrastructure.adapter}, que delega à
 * fatia dona do overlay (f006) — nem f005 nem f006 dependem uma da outra.
 */
@NullMarked
public interface TransactionCategoryOverlay {

    /** IDs das transações da pessoa vinculadas a qualquer categoria do conjunto. */
    List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds);

    /** Re-keya o overlay das transações da categoria de origem para a de destino (estratégia MOVE). */
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId);
}
