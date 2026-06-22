package br.community.feature.user.accounts.transactions;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserTransactionRepository {
    Optional<UserTransaction> findByTransactionAndUser(UUID transactionId, UUID userId);
    List<UserTransaction> findAllByUser(UUID userId);
    UserTransaction save(UserTransaction userTransaction);
    void deleteByTransaction(UUID transactionId);
    void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID userId);
}
