package br.cdb.feature.f006._0_domain.repository;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta do vínculo transação↔categoria ({@code F006_TRANSACTION_CATEGORY}) — tabela à parte de
 * {@code F006_TRANSACTION}. Dona é {@code f006} (a transação), não {@code f005} (dona só de
 * {@code F005_CATEGORY}, a categoria em si) — mesmo molde de {@link TransactionTagRepository}.
 */
@NullMarked
public interface TransactionCategoryRepository {
    /** Upsert do vínculo; {@code categoryId} nulo apaga a linha (a coluna é NOT NULL). */
    void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId);

    /** Categoria da transação, se houver vínculo para {@code personId}. */
    Optional<UUID> findCategory(UUID transactionId, UUID personId);

    /** Categoria por transação, para todas as transações vinculadas de {@code personId}. */
    Map<UUID, UUID> findCategoriesByPerson(UUID personId);

    /** Apaga o vínculo da transação, de qualquer pessoa (cascata de exclusão). */
    void deleteCategoryByTransaction(UUID transactionId);

    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId);

    /** Ids de transação do usuário classificadas em qualquer uma de {@code categoryIds}. */
    List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds);
}
