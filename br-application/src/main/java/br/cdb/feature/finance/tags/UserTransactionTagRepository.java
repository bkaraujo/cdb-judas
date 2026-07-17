package br.cdb.feature.finance.tags;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/** Porta da tabela de junção {@code USER_TRANSACTION_TAG} (transação × tag, N:N). */
@NullMarked
public interface UserTransactionTagRepository {
    List<UUID> findTransactionIdsByTag(UUID userId, UUID tagId);

    /** Reatribui os vínculos de {@code oldTagId} para {@code newTagId} (dedupe-safe: descarta o
     *  vínculo antigo quando a transação já está associada ao destino). */
    void reassignTag(UUID oldTagId, UUID newTagId, UUID userId);

    void deleteByTag(UUID userId, UUID tagId);

    void deleteByTransaction(UUID transactionId);
}
