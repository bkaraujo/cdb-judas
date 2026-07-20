package br.cdb.feature.finance.accounts.transactions;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTransactionService {

    private final UserTransactionRepository repo;

    public UserTransaction save(UUID transactionId, UUID accountId, UUID personId, @Nullable UUID categoryId) {
        return repo.save(new UserTransaction(transactionId, personId, accountId, categoryId, null, null));
    }

    public Optional<UserTransaction> find(UUID transactionId, UUID accountId, UUID personId) {
        return repo.findByTransactionAccountAndPerson(transactionId, accountId, personId);
    }

    public List<UserTransaction> findAllByPerson(UUID personId) {
        return repo.findAllByPerson(personId);
    }

    public Map<UUID, UserTransaction> indexByTransaction(UUID personId) {
        return findAllByPerson(personId).stream()
                .collect(Collectors.toMap(UserTransaction::transactionId, ut -> ut));
    }

    public void deleteByTransaction(UUID transactionId) {
        repo.deleteByTransaction(transactionId);
    }

    public void deleteByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId) {
        repo.deleteByTransactionAccountAndPerson(transactionId, accountId, personId);
    }

    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        repo.reassignCategory(oldCategoryId, newCategoryId, personId);
    }

    public void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId) {
        repo.reassignAccount(oldAccountId, newAccountId, personId);
    }

    public void deleteByAccountAndPerson(UUID accountId, UUID personId) {
        repo.deleteByAccountAndPerson(accountId, personId);
    }

    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return repo.findTransactionIdsByCategories(personId, categoryIds);
    }
}
