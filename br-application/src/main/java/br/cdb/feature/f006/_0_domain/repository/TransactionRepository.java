package br.cdb.feature.f006._0_domain.repository;

import br.cdb.feature.f006._0_domain.model.Transaction;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    // ── Vínculo transação↔categoria (F005_TRANSACTION_CATEGORY) ────────────────
    // Tabela à parte de F006_TRANSACTION: não entra no save(Transaction), tem escrita própria.

    /** Upsert do vínculo; {@code categoryId} nulo apaga a linha (a coluna é NOT NULL). */
    void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId);

    /** Categoria da transação, se houver vínculo para {@code personId}. */
    Optional<UUID> findCategory(UUID transactionId, UUID personId);

    /** Categoria por transação, para todas as transações vinculadas de {@code personId}. */
    Map<UUID, UUID> findCategoriesByPerson(UUID personId);

    /** Apaga o vínculo da transação, de qualquer pessoa (cascata de exclusão). */
    void deleteCategoryByTransaction(UUID transactionId);

    /** Ids de transação do usuário classificadas em qualquer uma de {@code categoryIds}. */
    List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds);
}
