package br.cdb.feature.f005._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserTransactionRepository {
    Optional<UserTransaction> findByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId);
    List<UserTransaction> findAllByPerson(UUID personId);
    UserTransaction save(UserTransaction userTransaction);
    void deleteByTransaction(UUID transactionId);
    void deleteByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId);
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId);

    /** Re-key em massa: move o overlay de {@code oldAccountId} para {@code newAccountId} (conta faz parte do PK). */
    void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId);

    void deleteByAccountAndPerson(UUID accountId, UUID personId);

    /** Ids de transação do usuário classificadas em qualquer uma de {@code categoryIds}. */
    List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds);
}
