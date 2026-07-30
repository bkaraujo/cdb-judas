package br.cdb.feature.f006._0_domain.repository;

import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface TransactionRepository extends Repository<Transaction, UUID> {
    /** Bulk re-key: move every transaction from {@code from} to {@code to} in one statement. */
    void reassignAccount(UUID from, UUID to);

    /** Bulk re-key: move every transaction carrying card {@code from} to card {@code to}. */
    void reassignCard(UUID from, UUID to);
    
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId);

    /** Re-key do vínculo de tag (MOVE): dedupe-safe, a transação que já tem o destino só perde a origem. */
    void reassignTag(UUID oldTagId, UUID newTagId, UUID personId);

    /** Desvincula a tag das transações (DETACH) — as transações permanecem intactas. */
    void detachTag(UUID tagId, UUID personId);

    /** Guarda implícita: só as transações de {@code personId} — F006_TRANSACTION.COD_PERSON no WHERE. */
    List<Transaction> findAllByPerson(String personId);

    /** Guarda implícita: vazio se {@code id} existe mas não pertence a {@code personId} (404 natural). */
    Optional<Transaction> findByIdAndPerson(UUID id, String personId);
}
