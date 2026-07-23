package br.cdb.context.monetary;

import br.cdb.feature.f005._0_domain.UserTransaction;
import br.cdb.feature.f005._0_domain.UserTransactionRepository;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Fake in-memory de {@code PERSON_TRANSACTION} para os testes de import (contexto sem CDI/JDBC). */
@NullMarked
class InMemoryUserTransactions implements UserTransactionRepository {

    private final List<UserTransaction> store = new ArrayList<>();

    List<UserTransaction> all() {
        return List.copyOf(store);
    }

    @Override
    public Optional<UserTransaction> findByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId) {
        return store.stream()
                .filter(u -> u.transactionId().equals(transactionId)
                        && u.accountId().equals(accountId)
                        && u.personId().equals(personId))
                .findFirst();
    }

    @Override
    public List<UserTransaction> findAllByPerson(UUID personId) {
        return store.stream().filter(u -> u.personId().equals(personId)).toList();
    }

    @Override
    public UserTransaction save(UserTransaction userTransaction) {
        store.removeIf(u -> u.transactionId().equals(userTransaction.transactionId())
                && u.accountId().equals(userTransaction.accountId())
                && u.personId().equals(userTransaction.personId()));
        store.add(userTransaction);
        return userTransaction;
    }

    @Override
    public void deleteByTransaction(UUID transactionId) {
        store.removeIf(u -> u.transactionId().equals(transactionId));
    }

    @Override
    public void deleteByTransactionAccountAndPerson(UUID transactionId, UUID accountId, UUID personId) {
        store.removeIf(u -> u.transactionId().equals(transactionId)
                && u.accountId().equals(accountId)
                && u.personId().equals(personId));
    }

    @Override
    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        // não exercido por estes testes
    }

    @Override
    public void reassignAccount(UUID oldAccountId, UUID newAccountId, UUID personId) {
        // não exercido por estes testes
    }

    @Override
    public void deleteByAccountAndPerson(UUID accountId, UUID personId) {
        store.removeIf(u -> u.accountId().equals(accountId) && u.personId().equals(personId));
    }

    @Override
    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return store.stream()
                .filter(u -> u.personId().equals(personId) && u.categoryId() != null && categoryIds.contains(u.categoryId()))
                .map(UserTransaction::transactionId)
                .toList();
    }
}
