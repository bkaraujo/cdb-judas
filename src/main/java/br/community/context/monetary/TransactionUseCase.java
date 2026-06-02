package br.community.context.monetary;

import br.commons.MessageBus;
import br.commons.Result;
import br.community.context.monetary._0_domain.event.MonetaryEvent;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.context.monetary._1_application.service.CategoryService;
import br.community.context.monetary._1_application.service.ClosingService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class TransactionUseCase {
    private final TransactionService transactionService;
    private final ClosingService closingService;
    private final CategoryService categoryService;

    public Result<List<MonetaryTransaction>, DomainError> listTransactions() {
        val all = transactionService.findAll().stream()
                .sorted(Comparator.comparing(MonetaryTransaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    public Result<List<MonetaryTransaction>, DomainError> listPendingTransactions() {
        return Result.success(transactionService.findPending());
    }

    public Result<MonetaryTransaction, DomainError> createTransaction(TransactionCommand cmd) {
        val catValidation = categoryService.validateNotMacroCategory(cmd.categoryId());
        if (catValidation instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.failure(error);

        val installmentsCount = cmd.installments() != null && cmd.installments() > 1 ? cmd.installments() : 1;

        if (installmentsCount == 1) {
            return closingService.validateDate(cmd.date())
                    .map(ignored -> {
                        val saved = transactionService.save(toMonetaryTransactionEntity(UUID.randomUUID(), cmd, cmd.date(), cmd.status(), null, null, null));
                        MessageBus.submit(new MonetaryEvent.TransactionCreated(saved));
                        return saved;
                    });
        }

        val groupId = UUID.randomUUID();
        val batch = new ArrayList<MonetaryTransaction>();

        for (int i = 1; i <= installmentsCount; i++) {
            val date = cmd.date().plusMonths(i - 1);
            val status = (i == 1) ? cmd.status() : "pending";
            val valResult = closingService.validateDate(date);
            if (valResult instanceof Result.Failure<Void, DomainError>(DomainError error)) {
                return Result.failure(error);
            }
            batch.add(toMonetaryTransactionEntity(UUID.randomUUID(), cmd, date, status, groupId, i, installmentsCount));
        }

        MonetaryTransaction first = null;
        for (val t : batch) {
            val saved = transactionService.save(t);
            if (first == null) first = saved;
        }

        if (first != null) MessageBus.submit(new MonetaryEvent.TransactionCreated(first));

        return Result.success(first);
    }

    public Result<MonetaryTransaction, DomainError> updateTransaction(UUID id, TransactionCommand cmd) {
        val catValidation = categoryService.validateNotMacroCategory(cmd.categoryId());
        if (catValidation instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.failure(error);

        return transactionService.findById(id).flatMap(existing -> {
            val isFuture = "FUTURE".equalsIgnoreCase(cmd.editMode()) && existing.groupId() != null;

            if (!isFuture) {
                return closingService.validateDate(existing.date())
                        .flatMap(ignored -> closingService.validateDate(cmd.date()))
                        .map(ignored -> {
                            val updated = transactionService.save(toMonetaryTransactionEntity(id, cmd, cmd.date(), cmd.status(),
                                    existing.groupId(), existing.installmentNumber(), existing.totalInstallments()));
                            MessageBus.submit(new MonetaryEvent.TransactionUpdated(existing));
                            MessageBus.submit(new MonetaryEvent.TransactionUpdated(updated));
                            return updated;
                        });
            }

            val groupId = existing.groupId();
            val installmentNumber = existing.installmentNumber();
            if (groupId == null || installmentNumber == null) return Result.<MonetaryTransaction, DomainError>success(existing);

            val all = transactionService.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() != null && t.installmentNumber() >= installmentNumber)
                    .sorted(Comparator.comparing(MonetaryTransaction::installmentNumber))
                    .toList();

            MonetaryTransaction firstSaved = null;
            for (val t : all) {
                Integer currentNumber = t.installmentNumber();
                if (currentNumber == null) continue;
                val newDate = cmd.date().plusMonths(currentNumber - installmentNumber);
                val valRes = closingService.validateDate(t.date()).flatMap(ig -> closingService.validateDate(newDate));
                if (valRes instanceof Result.Failure<Void, DomainError>(DomainError error)) {
                    return Result.<MonetaryTransaction>failure(error);
                }
                val updated = transactionService.save(toMonetaryTransactionEntity(t.id(), cmd, newDate, t.status(),
                        t.groupId(), t.installmentNumber(), t.totalInstallments()));
                if (t.id().equals(id)) firstSaved = updated;
            }

            if (firstSaved != null) {
                MessageBus.submit(new MonetaryEvent.TransactionUpdated(existing));
                MessageBus.submit(new MonetaryEvent.TransactionUpdated(firstSaved));
            }

            return Result.success(firstSaved);
        });
    }

    public Result<MonetaryTransaction, DomainError> updateTransactionStatus(UUID id, String status, @Nullable LocalDate paymentDate) {
        return transactionService.findById(id)
                .map(existing -> {
                    val saved = transactionService.save(new MonetaryTransaction(
                            existing.id(), existing.description(), existing.amount(), existing.date(),
                            existing.categoryId(), existing.accountId(), status, existing.type(), paymentDate,
                            existing.groupId(), existing.installmentNumber(), existing.totalInstallments()
                    ));
                    MessageBus.submit(new MonetaryEvent.TransactionUpdated(saved));
                    return saved;
                });
    }

    public Result<Void, DomainError> deleteTransaction(UUID id, @Nullable String mode) {
        return transactionService.findById(id).flatMap(existing -> {
            val isFuture = "FUTURE".equalsIgnoreCase(mode) && existing.groupId() != null;

            if (!isFuture) {
                return closingService.validateDate(existing.date())
                        .flatMap(ignored -> transactionService.deleteById(id))
                        .ifSuccess(ignored -> MessageBus.submit(new MonetaryEvent.TransactionDeleted(existing)));
            }

            val groupId = existing.groupId();
            val installmentNumber = existing.installmentNumber();
            if (groupId == null || installmentNumber == null) {
                return transactionService.deleteById(id)
                        .ifSuccess(ignored -> MessageBus.submit(new MonetaryEvent.TransactionDeleted(existing)));
            }

            val toDelete = transactionService.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() != null && t.installmentNumber() >= installmentNumber)
                    .toList();

            for (val t : toDelete) {
                val valRes = closingService.validateDate(t.date());
                if (valRes instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.<Void>failure(error);
            }

            for (val t : toDelete) {
                val delRes = transactionService.deleteById(t.id());
                if (delRes instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.<Void>failure(error);
            }

            MessageBus.submit(new MonetaryEvent.TransactionDeleted(existing));
            return Result.success();
        });
    }

    public Result<MonetaryTransaction, DomainError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            return Result.failure(new DomainError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }
        val validation = closingService.validateDate(date);
        if (validation instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.failure(error);

        val transferCat = categoryService.findOrCreateTransferCategory();
        val groupId = UUID.randomUUID();
        val absAmount = amount.abs();
        val outId = UUID.randomUUID();
        val inId = UUID.randomUUID();

        val outflow = new MonetaryTransaction(
                outId, "Transferência (saída)", absAmount.negate(), date,
                transferCat.id(), fromAccountId, "confirmed", "expense", date,
                groupId, 1, 2
        );
        val inflow = new MonetaryTransaction(
                inId, "Transferência (entrada)", absAmount, date,
                transferCat.id(), toAccountId, "confirmed", "income", date,
                groupId, 2, 2
        );

        val savedOut = transactionService.save(outflow);
        val savedIn = transactionService.save(inflow);
        MessageBus.submit(new MonetaryEvent.TransactionCreated(savedOut));
        MessageBus.submit(new MonetaryEvent.TransactionCreated(savedIn));
        return Result.success(savedOut);
    }

    /** Persists an already-resolved imported movement (sign applied here from {@code type}) and emits
     *  the creation event so balances recalc. Used by the statement-import feature via the facade. */
    public Result<MonetaryTransaction, DomainError> createImported(ImportedTransactionCommand cmd) {
        final BigDecimal signed = "income".equals(cmd.type())
                ? cmd.amount().abs()
                : cmd.amount().abs().negate();
        val tx = new MonetaryTransaction(
                UUID.randomUUID(), cmd.description(), signed, cmd.date(),
                cmd.categoryId(), cmd.accountId(), cmd.status(), cmd.type(), null,
                cmd.groupId(), cmd.installmentNumber(), cmd.totalInstallments());
        val saved = transactionService.save(tx);
        MessageBus.submit(new MonetaryEvent.TransactionCreated(saved));
        return Result.success(saved);
    }

    private MonetaryTransaction toMonetaryTransactionEntity(UUID id, TransactionCommand cmd, LocalDate date, String status,
                                                            @Nullable UUID groupId, @Nullable Integer installmentNumber, @Nullable Integer totalInstallments) {
        return new MonetaryTransaction(id, cmd.description(), cmd.amount(), date,
                cmd.categoryId(), cmd.accountId(), status, cmd.type(), null,
                groupId, installmentNumber, totalInstallments);
    }

}
