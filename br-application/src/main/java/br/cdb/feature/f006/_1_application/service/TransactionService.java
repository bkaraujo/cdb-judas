package br.cdb.feature.f006._1_application.service;

import br.cdb.feature.f006._0_domain.model.Transaction;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.commons.framework.cdi.Context;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public class TransactionService {

    private final TransactionRepository repository = Context.get(TransactionRepository.class);

    public List<Transaction> findAll() {
        return repository.findAll();
    }

    public Result<Transaction, BusinessError> findById(UUID id) {
        return repository.findById(id)
                .<Result<Transaction, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("TransactionResponse not found: " + id)));
    }

    public List<Transaction> findAllByPerson(String personId) {
        return repository.findAllByPerson(personId);
    }

    public Result<Transaction, BusinessError> findByIdAndPerson(UUID id, String personId) {
        return repository.findByIdAndPerson(id, personId)
                .<Result<Transaction, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Transaction not found: " + id)));
    }

    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    public Result<Void, BusinessError> deleteById(UUID id) {
        repository.deleteById(id);
        return Result.success();
    }

    public void reassignAccount(UUID from, UUID to) {
        repository.reassignAccount(from, to);
    }

    public void reassignCard(UUID from, UUID to) {
        repository.reassignCard(from, to);
    }

    // ── Vínculo transação↔categoria (F005_TRANSACTION_CATEGORY) ────────────────

    public void saveCategory(UUID transactionId, UUID personId, @Nullable UUID categoryId) {
        repository.saveCategory(transactionId, personId, categoryId);
    }

    public Optional<UUID> findCategory(UUID transactionId, UUID personId) {
        return repository.findCategory(transactionId, personId);
    }

    public Map<UUID, UUID> findCategoriesByPerson(UUID personId) {
        return repository.findCategoriesByPerson(personId);
    }

    public void deleteCategoryByTransaction(UUID transactionId) {
        repository.deleteCategoryByTransaction(transactionId);
    }

    public void reassignCategory(UUID oldCategoryId, UUID newCategoryId, UUID personId) {
        repository.reassignCategory(oldCategoryId, newCategoryId, personId);
    }

    public List<UUID> findTransactionIdsByCategories(UUID personId, Collection<UUID> categoryIds) {
        return repository.findTransactionIdsByCategories(personId, categoryIds);
    }

    public List<Transaction> findByAccount(UUID accountId) {
        return repository.findAll().stream()
                .filter(t -> accountId.equals(t.accountId()))
                .toList();
    }

    public List<Transaction> findByCard(UUID cardId) {
        return repository.findAll().stream()
                .filter(t -> cardId.equals(t.cardId()))
                .toList();
    }

    public List<Transaction> findPending() {
        return repository.findAll().stream()
                .filter(t -> Transaction.Status.PENDING.equals(t.status()))
                .sorted(Comparator.comparing(Transaction::date))
                .toList();
    }

    public List<Transaction> findByGroupId(UUID groupId) {
        return repository.findAll().stream()
                .filter(t -> groupId.equals(t.groupId()))
                .toList();
    }

    /** Returns the opposite leg(s) of a transfer when {@code tx} belongs to a transfer group.
     *  A transfer group mixes one income and one expense leg under a shared groupId, unlike an
     *  installment group whose members share a single type. Empty when {@code tx} is not a transfer. */
    public List<Transaction> findTransferSiblings(Transaction tx) {
        if (tx.groupId() == null) return List.of();
        val group = findByGroupId(tx.groupId());
        val hasIncome = group.stream().anyMatch(t -> Transaction.Type.INCOME.equals(t.type()));
        val hasExpense = group.stream().anyMatch(t -> Transaction.Type.EXPENSE.equals(t.type()));
        if (!hasIncome || !hasExpense) return List.of();
        return group.stream().filter(t -> !t.id().equals(tx.id())).toList();
    }
}
