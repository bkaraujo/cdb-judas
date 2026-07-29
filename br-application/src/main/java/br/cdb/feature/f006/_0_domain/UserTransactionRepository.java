package br.cdb.feature.f006._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserTransactionRepository {
    Optional<UserTransaction> findByTransactionAndPerson(UUID transactionId, UUID personId);
    List<UserTransaction> findAllByPerson(UUID personId);
    UserTransaction save(UserTransaction userTransaction);
    void deleteByTransaction(UUID transactionId);
    void deleteByTransactionAndPerson(UUID transactionId, UUID personId);
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId);

    /** Ids de transação do usuário classificadas em qualquer uma de {@code categoryIds}. */
    List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds);
}
