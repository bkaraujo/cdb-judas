package br.community.feature.user.accounts.transactions;

import org.jspecify.annotations.NullMarked;

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
}
