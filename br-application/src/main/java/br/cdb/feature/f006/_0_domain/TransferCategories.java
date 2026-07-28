package br.cdb.feature.f006._0_domain;

import br.cdb.context.monetary._0_domain.model.Transaction;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Porta que f006 declara para resolver a categoria de sistema de transferência sem depender de
 * f005 diretamente. Implementada por um adapter em {@code f999._2_infrastructure.adapter}, que
 * delega a {@code f005.UserCategoryService} — f999 é o único lugar do código que conhece os dois
 * lados. {@link Transaction.Type} é modelo do contexto monetário, permitido nas duas fatias.
 */
@NullMarked
public interface TransferCategories {
    UUID transferCategoryId(UUID personId, Transaction.Type nature);
}
