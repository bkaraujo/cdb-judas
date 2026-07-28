package br.cdb.feature.f006._0_domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Porta que f006 declara para persistir o overlay {@code PERSON_TRANSACTION} da transação
 * importada sem depender de f005 diretamente. Só primitivos — zero tipos cross-slice. Implementada
 * por um adapter em {@code f999._2_infrastructure.adapter}, que delega a
 * {@code f005.UserTransactionService.save}.
 */
@NullMarked
public interface TransactionOverlaySink {
    void save(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId);
}
