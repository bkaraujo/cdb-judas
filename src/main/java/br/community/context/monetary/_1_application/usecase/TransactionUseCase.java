package br.community.context.monetary._1_application.usecase;

import br.commons.MessageBus;
import br.commons.Result;
import br.community.context.monetary._0_domain.event.TransactionEvents;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.context.monetary._1_application.service.CategoryService;
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
    private final CategoryService categoryService;

    public Result<List<Transaction>, DomainError> listTransactions() {
        val all = transactionService.findAll().stream()
                .sorted(Comparator.comparing(Transaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    public Result<List<Transaction>, DomainError> listPendingTransactions() {
        return Result.success(transactionService.findPending());
    }

    public Result<Transaction, DomainError> findTransaction(UUID id) {
        return transactionService.findById(id);
    }

    public Result<Transaction, DomainError> createTransaction(TransactionCommand cmd) {
        return categoryService.validateNotMacroCategory(cmd.categoryId())
                .flatMap(ignored -> {
                    val count = installmentCount(cmd);
                    return count == 1 ? createSingle(cmd) : createInstallments(cmd, count);
                });
    }

    private static int installmentCount(TransactionCommand cmd) {
        return cmd.installments() != null && cmd.installments() > 1 ? cmd.installments() : 1;
    }

    private Result<Transaction, DomainError> createSingle(TransactionCommand cmd) {
        val saved = transactionService.save(toMonetaryTransactionEntity(UUID.randomUUID(), cmd, cmd.date(), cmd.status(), null, null, null));
        MessageBus.submit(new TransactionEvents.Created(saved));
        return Result.success(saved);
    }

    private Result<Transaction, DomainError> createInstallments(TransactionCommand cmd, int installmentsCount) {
        val groupId = UUID.randomUUID();
        val batch = new ArrayList<Transaction>();

        for (int i = 1; i <= installmentsCount; i++) {
            val date = cmd.date().plusMonths(i - 1);
            val status = (i == 1) ? cmd.status() : Transaction.Status.PENDING;
            batch.add(toMonetaryTransactionEntity(UUID.randomUUID(), cmd, date, status, groupId, i, installmentsCount));
        }

        Transaction first = null;
        for (val t : batch) {
            val saved = transactionService.save(t);
            if (first == null) first = saved;
        }

        if (first != null) MessageBus.submit(new TransactionEvents.Created(first));

        return Result.success(first);
    }

    public Result<Transaction, DomainError> updateTransaction(UUID id, TransactionCommand cmd) {
        return categoryService.validateNotMacroCategory(cmd.categoryId())
                .flatMap(catOk -> transactionService.findById(id).flatMap(existing -> {
            val transferSiblings = transactionService.findTransferSiblings(existing);
            if (!transferSiblings.isEmpty()) return updateTransfer(existing, transferSiblings, cmd);

            val isFuture = "FUTURE".equalsIgnoreCase(cmd.editMode()) && existing.groupId() != null;

            if (!isFuture) {
                // A standalone entry, or a single-mode edit of an installment, keeps its own group metadata.
                val updated = transactionService.save(toMonetaryTransactionEntity(id, cmd, cmd.date(), cmd.status(),
                        existing.groupId(), existing.installmentNumber(), existing.totalInstallments()));
                MessageBus.submit(new TransactionEvents.Updated(existing));
                MessageBus.submit(new TransactionEvents.Updated(updated));
                return Result.<Transaction, DomainError>success(updated);
            }

            val groupId = existing.groupId();
            val installmentNumber = existing.installmentNumber();
            if (groupId == null) return Result.<Transaction, DomainError>success(existing);

            val all = transactionService.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() >= installmentNumber)
                    .sorted(Comparator.comparing(Transaction::installmentNumber))
                    .toList();

            Transaction firstSaved = null;
            for (val t : all) {
                val currentNumber = t.installmentNumber();
                val newDate = cmd.date().plusMonths(currentNumber - installmentNumber);
                val updated = transactionService.save(toMonetaryTransactionEntity(t.id(), cmd, newDate, t.status(),
                        t.groupId(), t.installmentNumber(), t.totalInstallments()));
                if (t.id().equals(id)) firstSaved = updated;
            }

            if (firstSaved != null) {
                MessageBus.submit(new TransactionEvents.Updated(existing));
                MessageBus.submit(new TransactionEvents.Updated(firstSaved));
            }

            return Result.success(firstSaved);
        }));
    }

    /** A transfer stays an inseparable pair when edited: date, amount magnitude and status mirror to
     *  both legs (each keeps the sign dictated by its own type), while the account change applies only
     *  to the edited leg — moving that side of the transfer. The opposite leg keeps its own account.
     *  Description, category, cost center, type and group metadata are preserved per leg. */
    private Result<Transaction, DomainError> updateTransfer(Transaction edited, List<Transaction> siblings, TransactionCommand cmd) {
        val newAccount = cmd.accountId();
        if (siblings.stream().anyMatch(sib -> newAccount.equals(sib.accountId()))) {
            return Result.failure(new DomainError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val absAmount = cmd.amount().abs();
        val updatedEdited = transactionService.save(withTransferEdits(edited, newAccount, absAmount, cmd.date(), cmd.status()));
        // The pre-edit snapshot recalculates the leg's former account when the account moved.
        MessageBus.submit(new TransactionEvents.Updated(edited));
        MessageBus.submit(new TransactionEvents.Updated(updatedEdited));

        for (val sib : siblings) {
            val updatedSib = transactionService.save(withTransferEdits(sib, sib.accountId(), absAmount, cmd.date(), cmd.status()));
            MessageBus.submit(new TransactionEvents.Updated(updatedSib));
        }
        return Result.success(updatedEdited);
    }

    /** Rebuilds a transfer leg keeping its identity, description, category, cost center, type and group
     *  metadata; applies the (possibly new) account, the shared date/status and the signed amount. A
     *  confirmed leg keeps {@code paymentDate} aligned with its date, matching how transfers are created. */
    private static Transaction withTransferEdits(Transaction leg, UUID accountId, BigDecimal absAmount, LocalDate date, Transaction.Status status) {
        val signed = Transaction.Type.EXPENSE.equals(leg.type()) ? absAmount.negate() : absAmount;
        return new Transaction(
                leg.id(), leg.description(), signed, date,
                leg.categoryId(), accountId, status, leg.type(), leg.costCenterId(),
                Transaction.Status.CONFIRMED.equals(status) ? date : null,
                leg.groupId(), leg.installmentNumber(), leg.totalInstallments(), leg.notes());
    }

    public Result<Transaction, DomainError> updateTransactionStatus(UUID id, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return transactionService.findById(id)
                .map(existing -> {
                    val saved = transactionService.save(new Transaction(
                            existing.id(), existing.description(), existing.amount(), existing.date(),
                            existing.categoryId(), existing.accountId(), status, existing.type(), existing.costCenterId(), paymentDate,
                            existing.groupId(), existing.installmentNumber(), existing.totalInstallments(), existing.notes()
                    ));
                    MessageBus.submit(new TransactionEvents.Updated(saved));
                    return saved;
                });
    }

    public Result<Void, DomainError> deleteTransaction(UUID id, @Nullable String mode) {
        return transactionService.findById(id).flatMap(existing -> {
            val transferSiblings = transactionService.findTransferSiblings(existing);
            if (!transferSiblings.isEmpty()) return deleteTransferGroup(existing, transferSiblings);

            val isFuture = "FUTURE".equalsIgnoreCase(mode) && existing.groupId() != null;

            if (!isFuture) {
                return transactionService.deleteById(id)
                        .ifSuccess(ignored -> MessageBus.submit(new TransactionEvents.Deleted(existing)));
            }

            val groupId = existing.groupId();
            val installmentNumber = existing.installmentNumber();
            if (groupId == null) {
                return transactionService.deleteById(id)
                        .ifSuccess(ignored -> MessageBus.submit(new TransactionEvents.Deleted(existing)));
            }

            val toDelete = transactionService.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() >= installmentNumber)
                    .toList();

            for (val t : toDelete) {
                val delRes = transactionService.deleteById(t.id());
                if (delRes instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.<Void>failure(error);
            }

            MessageBus.submit(new TransactionEvents.Deleted(existing));
            return Result.success();
        });
    }

    /** A transfer is an inseparable pair: removing either leg removes both so balances on both
     *  accounts stay consistent. Delete mode (SINGLE/FUTURE/ALL) is irrelevant for transfers. */
    private Result<Void, DomainError> deleteTransferGroup(Transaction leg, List<Transaction> siblings) {
        val legs = new ArrayList<Transaction>();
        legs.add(leg);
        legs.addAll(siblings);
        for (val t : legs) {
            if (transactionService.deleteById(t.id()) instanceof Result.Failure<Void, DomainError>(DomainError error)) {
                return Result.failure(error);
            }
        }
        for (val t : legs) MessageBus.submit(new TransactionEvents.Deleted(t));
        return Result.success();
    }

    public Result<Transaction, DomainError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            return Result.failure(new DomainError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val transferCat = categoryService.findOrCreateTransferCategory();
        val groupId = UUID.randomUUID();
        val absAmount = amount.abs();
        val outId = UUID.randomUUID();
        val inId = UUID.randomUUID();

        val outflow = new Transaction(
                outId, "Transferência (saída)", absAmount.negate(), date,
                transferCat.id(), fromAccountId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL_ID, date,
                groupId, 1, 2, null
        );
        val inflow = new Transaction(
                inId, "Transferência (entrada)", absAmount, date,
                transferCat.id(), toAccountId, Transaction.Status.CONFIRMED, Transaction.Type.INCOME, CostCenter.VARIAVEL_ID, date,
                groupId, 2, 2, null
        );

        val savedOut = transactionService.save(outflow);
        val savedIn = transactionService.save(inflow);
        MessageBus.submit(new TransactionEvents.Created(savedOut));
        MessageBus.submit(new TransactionEvents.Created(savedIn));
        return Result.success(savedOut);
    }

    /** Persists an already-resolved imported movement (sign applied here from {@code type}) and emits
     *  the creation event so balances recalc. Used by the statement-import feature via the facade. */
    public Result<Transaction, DomainError> createImported(ImportedTransactionCommand cmd) {
        val signed = Transaction.Type.INCOME.equals(cmd.type())
                ? cmd.amount().abs()
                : cmd.amount().abs().negate();
        val tx = new Transaction(
                UUID.randomUUID(), cmd.description(), signed, cmd.date(),
                cmd.categoryId(), cmd.accountId(), cmd.status(), cmd.type(), CostCenter.VARIAVEL_ID, null,
                cmd.groupId(), installmentOrDefault(cmd.installmentNumber()), installmentOrDefault(cmd.totalInstallments()), null);
        val saved = transactionService.save(tx);
        MessageBus.submit(new TransactionEvents.Created(saved));
        return Result.success(saved);
    }

    private Transaction toMonetaryTransactionEntity(UUID id, TransactionCommand cmd, LocalDate date, Transaction.Status status,
                                                            @Nullable UUID groupId, @Nullable Integer installmentNumber, @Nullable Integer totalInstallments) {
        return new Transaction(id, cmd.description(), cmd.amount(), date,
                cmd.categoryId(), cmd.accountId(), status, cmd.type(), cmd.costCenterId(), null,
                groupId, installmentOrDefault(installmentNumber), installmentOrDefault(totalInstallments), cmd.notes());
    }

    /** Installment metadata is optional upstream (à-vista charges and standalone entries carry none); the
     *  domain models a single charge as installment 1 of 1, so an absent value resolves to 1. */
    private static int installmentOrDefault(@Nullable Integer value) {
        return value == null ? 1 : value;
    }

}
