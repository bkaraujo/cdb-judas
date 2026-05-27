package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<MonetaryTransaction> findAll() {
        return transactionRepository.findAll();
    }

    public Result<MonetaryTransaction, DomainError> findById(UUID id) {
        return transactionRepository.findById(id)
                .<Result<MonetaryTransaction, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Transaction not found: " + id)));
    }

    public MonetaryTransaction save(MonetaryTransaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Result<Void, DomainError> deleteById(UUID id) {
        transactionRepository.deleteById(id);
        return Result.success();
    }

    public List<MonetaryTransaction> findByAccount(UUID accountId) {
        return transactionRepository.findAll().stream()
                .filter(t -> accountId.equals(t.accountId()))
                .toList();
    }

    public List<MonetaryTransaction> findPending() {
        return transactionRepository.findAll().stream()
                .filter(t -> "pending".equalsIgnoreCase(t.status()))
                .sorted(Comparator.comparing(MonetaryTransaction::date))
                .toList();
    }

    public List<MonetaryTransaction> findByGroupId(UUID groupId) {
        return transactionRepository.findAll().stream()
                .filter(t -> groupId.equals(t.groupId()))
                .toList();
    }
}
