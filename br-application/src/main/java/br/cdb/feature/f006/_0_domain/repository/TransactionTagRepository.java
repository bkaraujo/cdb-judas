package br.cdb.feature.f006._0_domain.repository;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Porta do vínculo transação↔tag ({@code F006_TRANSACTION_TAG}) — tabela à parte de
 * {@code F006_TRANSACTION}. Dona é {@code f006} (a transação), não {@code f004} (dona só de
 * {@code F004_TAG}, a tag em si) — mesmo molde de {@link TransactionCategoryRepository}.
 */
@NullMarked
public interface TransactionTagRepository {
    /** Substitui todos os vínculos da transação por {@code tagIds} (DELETE + INSERT). */
    void replaceTags(UUID transactionId, UUID personId, List<UUID> tagIds);

    /** Tags por transação, para todas as transações vinculadas de {@code personId}. */
    Map<UUID, List<UUID>> findTagsByPerson(UUID personId);

    /** Apaga todos os vínculos da transação, de qualquer pessoa (cascata de exclusão). */
    void deleteTagsByTransaction(UUID transactionId);

    /** Re-key do vínculo (MOVE): dedupe-safe, a transação que já tem o destino só perde a origem. */
    void reassignTag(UUID oldTagId, UUID newTagId, UUID personId);

    /** Desvincula a tag das transações (DETACH) — as transações permanecem intactas. */
    void detachTag(UUID tagId, UUID personId);
}
