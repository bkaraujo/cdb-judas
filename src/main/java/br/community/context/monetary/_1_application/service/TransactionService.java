package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Result<Transaction, DomainError> findById(UUID id) {
        return transactionRepository.findById(id)
                .<Result<Transaction, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("TransactionResponse not found: " + id)));
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Result<Void, DomainError> deleteById(UUID id) {
        transactionRepository.deleteById(id);
        return Result.success();
    }

    public List<Transaction> findByAccount(UUID accountId) {
        return transactionRepository.findAll().stream()
                .filter(t -> accountId.equals(t.accountId()))
                .toList();
    }

    public List<Transaction> findByCard(UUID cardId) {
        return transactionRepository.findAll().stream()
                .filter(t -> cardId.equals(t.cardId()))
                .toList();
    }

    public List<Transaction> findPending() {
        return transactionRepository.findAll().stream()
                .filter(t -> Transaction.Status.PENDING.equals(t.status()))
                .sorted(Comparator.comparing(Transaction::date))
                .toList();
    }

    public List<Transaction> findByGroupId(UUID groupId) {
        return transactionRepository.findAll().stream()
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
