package br.cdb.feature.user.accounts.transactions;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserTransactionRepository {
    Optional<UserTransaction> findByTransactionAccountAndUser(UUID transactionId, UUID accountId, UUID userId);
    List<UserTransaction> findAllByUser(UUID userId);
    UserTransaction save(UserTransaction userTransaction);
    void deleteByTransaction(UUID transactionId);
    void deleteByTransactionAccountAndUser(UUID transactionId, UUID accountId, UUID userId);
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID userId);

    /** Re-key em massa: move o overlay de {@code oldAccountId} para {@code newAccountId} (conta faz parte do PK). */
    void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID userId);

    void deleteByAccountAndUser(UUID accountId, UUID userId);

    /** Ids de transação do usuário classificadas em qualquer uma de {@code categoryIds}. */
    List<UUID> findTransactionIdsByCategories(UUID userId, Collection<UUID> categoryIds);
}
