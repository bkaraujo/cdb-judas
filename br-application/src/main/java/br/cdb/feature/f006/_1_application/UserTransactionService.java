package br.cdb.feature.f006._1_application;

import br.cdb.feature.f006._0_domain.UserTransaction;
import br.cdb.feature.f006._0_domain.UserTransactionRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço do overlay de transações ({@code PERSON_TRANSACTION}). Os métodos de manutenção exigidos
 * pelas portas {@code TransactionAccountOverlay} (f002) e {@code TransactionCategoryOverlay} (f005)
 * são delegados por adapters em {@code f999._2_infrastructure.adapter} — f006 não implementa as
 * portas diretamente, para não depender de f002/f005 nem eles de f006.
 */
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
