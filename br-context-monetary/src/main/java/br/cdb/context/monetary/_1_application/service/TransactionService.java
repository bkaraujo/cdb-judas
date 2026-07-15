package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._0_domain.repository.TransactionRepository;
import br.cdb.context.monetary._1_application.event.TransactionEventListener;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
public class TransactionService {

    private final TransactionRepository transactionRepository = Registry.get(TransactionRepository.class);

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Result<Transaction, BusinessError> findById(UUID id) {
        return transactionRepository.findById(id)
                .<Result<Transaction, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("TransactionResponse not found: " + id)));
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Result<Void, BusinessError> deleteById(UUID id) {
        transactionRepository.deleteById(id);
        return Result.success();
    }

    public void reassignAccount(UUID from, UUID to) {
        transactionRepository.reassignAccount(from, to);
    }

    public void reassignCard(UUID from, UUID to) {
        transactionRepository.reassignCard(from, to);
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
