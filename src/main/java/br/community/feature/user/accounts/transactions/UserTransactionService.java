package br.community.feature.user.accounts.transactions;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTransactionService {

    private final UserTransactionRepository repo;

    public UserTransaction save(UUID transactionId, UUID accountId, UUID userId, @Nullable UUID categoryId) {
        return repo.save(new UserTransaction(transactionId, userId, accountId, categoryId, null, null));
    }

    public Optional<UserTransaction> find(UUID transactionId, UUID accountId, UUID userId) {
        return repo.findByTransactionAccountAndUser(transactionId, accountId, userId);
    }

    public List<UserTransaction> findAllByUser(UUID userId) {
        return repo.findAllByUser(userId);
    }

    public Map<UUID, UserTransaction> indexByTransaction(UUID userId) {
        return findAllByUser(userId).stream()
                .collect(Collectors.toMap(UserTransaction::transactionId, ut -> ut));
    }

    public void deleteByTransaction(UUID transactionId) {
        repo.deleteByTransaction(transactionId);
    }

    public void deleteByTransactionAccountAndUser(UUID transactionId, UUID accountId, UUID userId) {
        repo.deleteByTransactionAccountAndUser(transactionId, accountId, userId);
    }

    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID userId) {
        repo.reassignCategory(oldCategoryId, newCategoryId, userId);
    }
}
