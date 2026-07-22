package br.cdb.feature.f005._1_application;

import br.cdb.feature.f002._0_domain.TransactionAccountOverlay;
import br.cdb.feature.f004._0_domain.TransactionCategoryOverlay;
import br.cdb.feature.f005._0_domain.UserTransaction;
import br.cdb.feature.f005._0_domain.UserTransactionRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço do overlay de transações ({@code PERSON_TRANSACTION}). Implementa as portas de manutenção
 * de overlay exigidas pelas fatias anteriores {@link TransactionAccountOverlay} (f002) e
 * {@link TransactionCategoryOverlay} (f004): f005 depende delas (fatia posterior conhece as
 * anteriores), respeitando a ordem de fatias sem que f002/f004 dependam de f005.
 */
@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTransactionService implements TransactionAccountOverlay, TransactionCategoryOverlay {

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
