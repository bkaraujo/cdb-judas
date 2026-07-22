package br.cdb.feature.f003._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/** Porta da tabela de junção {@code PERSON_TRANSACTION_TAG} (transação × tag, N:N). */
@NullMarked
public interface UserTransactionTagRepository {
    List<UUID> findTransactionIdsByTag(UUID personId, UUID tagId);

    /** Reatribui os vínculos de {@code oldTagId} para {@code newTagId} (dedupe-safe: descarta o
     *  vínculo antigo quando a transação já está associada ao destino). */
    void reassignTag(UUID oldTagId, UUID newTagId, UUID personId);

    void deleteByTag(UUID personId, UUID tagId);

    void deleteByTransaction(UUID transactionId);
}
