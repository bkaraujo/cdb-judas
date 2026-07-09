package br.community.context.monetary._1_application.usecase;

import br.commons.MessageBus;
import br.commons.Result;
import br.community.context.monetary._0_domain.event.TransactionEvents;
import br.community.context.monetary._0_domain.model.CostCenter;
import br.community.context.monetary._0_domain.model.CreditCard;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.monetary._1_application.command.ImportedTransactionCommand;
import br.community.context.monetary._1_application.command.TransactionCommand;
import br.community.context.monetary._1_application.service.BalanceRecalculationService;
import br.community.context.monetary._1_application.service.CardService;
import br.community.context.monetary._1_application.service.TransactionService;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@NullMarked
@RequiredArgsConstructor
public class TransactionUseCase {
    private final TransactionService transactionService;
    private final CardService cardService;
    private final BalanceRecalculationService balanceRecalculationService;

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
        return validateCard(cmd.accountId(), cmd.cardId()).flatMap(ignored -> dispatchCreate(cmd));
    }

    private Result<Transaction, DomainError> dispatchCreate(TransactionCommand cmd) {
        val count = installmentCount(cmd);
        return count == 1 ? createSingle(cmd) : createInstallments(cmd, count);
    }

    private static int installmentCount(TransactionCommand cmd) {
        return cmd.installments() != null && cmd.installments() > 1 ? cmd.installments() : 1;
    }

    private Result<Transaction, DomainError> createSingle(TransactionCommand cmd) {
        val saved = transactionService.save(toEntity(UUID.randomUUID(), cmd, cmd.date(), cmd.status(), null, null, null));
        MessageBus.submit(new TransactionEvents.Created(saved));
        return Result.success(saved);
    }

    private Result<Transaction, DomainError> createInstallments(TransactionCommand cmd, int installmentsCount) {
        val groupId = UUID.randomUUID();
        val batch = new ArrayList<Transaction>();

        for (int i = 1; i <= installmentsCount; i++) {
            val date = cmd.date().plusMonths(i - 1);
            val status = (i == 1) ? cmd.status() : Transaction.Status.PENDING;
            batch.add(toEntity(UUID.randomUUID(), cmd, date, status, groupId, i, installmentsCount));
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
        return transactionService.findById(id).flatMap(existing -> dispatchUpdate(id, existing, cmd));
    }

    private Result<Transaction, DomainError> dispatchUpdate(UUID id, Transaction existing, TransactionCommand cmd) {
        val transferSiblings = transactionService.findTransferSiblings(existing);
        if (!transferSiblings.isEmpty()) return updateTransfer(existing, transferSiblings, cmd);
        return validateCard(cmd.accountId(), cmd.cardId()).flatMap(ignored -> updateNonTransfer(id, existing, cmd));
    }

    private Result<Transaction, DomainError> updateNonTransfer(UUID id, Transaction existing, TransactionCommand cmd) {
        val isFuture = "FUTURE".equalsIgnoreCase(cmd.editMode()) && existing.groupId() != null;

        if (!isFuture) {
            val updated = transactionService.save(toEntity(id, cmd, cmd.date(), cmd.status(),
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
            val updated = transactionService.save(toEntity(t.id(), cmd, newDate, t.status(),
                    t.groupId(), t.installmentNumber(), t.totalInstallments()));
            if (t.id().equals(id)) firstSaved = updated;
        }

        if (firstSaved != null) {
            MessageBus.submit(new TransactionEvents.Updated(existing));
            MessageBus.submit(new TransactionEvents.Updated(firstSaved));
        }

        return Result.success(firstSaved);
    }

    /** A transfer stays an inseparable pair when edited. */
    private Result<Transaction, DomainError> updateTransfer(Transaction edited, List<Transaction> siblings, TransactionCommand cmd) {
        val newAccount = cmd.accountId();
        if (siblings.stream().anyMatch(sib -> newAccount.equals(sib.accountId()))) {
            return Result.failure(new DomainError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val absAmount = cmd.amount().abs();
        val updatedEdited = transactionService.save(withTransferEdits(edited, newAccount, absAmount, cmd.date(), cmd.status()));
        MessageBus.submit(new TransactionEvents.Updated(edited));
        MessageBus.submit(new TransactionEvents.Updated(updatedEdited));

        for (val sib : siblings) {
            val updatedSib = transactionService.save(withTransferEdits(sib, sib.accountId(), absAmount, cmd.date(), cmd.status()));
            MessageBus.submit(new TransactionEvents.Updated(updatedSib));
        }
        return Result.success(updatedEdited);
    }

    /** Transfer legs never carry a card — cardId is always null. */
    private static Transaction withTransferEdits(Transaction leg, UUID accountId, BigDecimal absAmount, LocalDate date, Transaction.Status status) {
        return new Transaction(
                leg.id(), leg.description(), absAmount, date,
                accountId, status, leg.type(), leg.costCenterId(),
                Transaction.Status.CONFIRMED.equals(status) ? date : null,
                leg.groupId(), leg.installmentNumber(), leg.totalInstallments(), leg.notes(), null);
    }

    public Result<Transaction, DomainError> updateTransactionStatus(UUID id, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return transactionService.findById(id)
                .map(existing -> {
                    val saved = transactionService.save(new Transaction(
                            existing.id(), existing.description(), existing.amount(), existing.date(),
                            existing.accountId(), status, existing.type(), existing.costCenterId(), paymentDate,
                            existing.groupId(), existing.installmentNumber(), existing.totalInstallments(), existing.notes(),
                            existing.cardId()
                    ));
                    MessageBus.submit(new TransactionEvents.Updated(saved));
                    return saved;
                });
    }

    /** Ids das transações apagadas (par de transferência / lote FUTURE / unitário). */
    public Result<List<UUID>, DomainError> deleteTransaction(UUID id, @Nullable String mode) {
        return transactionService.findById(id).flatMap(existing -> {
            val transferSiblings = transactionService.findTransferSiblings(existing);
            if (!transferSiblings.isEmpty()) return deleteTransferGroup(existing, transferSiblings);

            val isFuture = "FUTURE".equalsIgnoreCase(mode) && existing.groupId() != null;
            val groupId = existing.groupId();

            if (!isFuture || groupId == null) {
                return transactionService.deleteById(id).map(ignored -> {
                    MessageBus.submit(new TransactionEvents.Deleted(existing));
                    return List.of(id);
                });
            }

            val installmentNumber = existing.installmentNumber();
            val toDelete = transactionService.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() >= installmentNumber)
                    .toList();

            for (val t : toDelete) {
                val delRes = transactionService.deleteById(t.id());
                if (delRes instanceof Result.Failure<Void, DomainError>(DomainError error)) return Result.<List<UUID>>failure(error);
            }

            MessageBus.submit(new TransactionEvents.Deleted(existing));
            return Result.success(toDelete.stream().map(Transaction::id).toList());
        });
    }

    private Result<List<UUID>, DomainError> deleteTransferGroup(Transaction leg, List<Transaction> siblings) {
        val legs = new ArrayList<Transaction>();
        legs.add(leg);
        legs.addAll(siblings);
        for (val t : legs) {
            if (transactionService.deleteById(t.id()) instanceof Result.Failure<Void, DomainError>(DomainError error)) {
                return Result.failure(error);
            }
        }
        for (val t : legs) MessageBus.submit(new TransactionEvents.Deleted(t));
        return Result.success(legs.stream().map(Transaction::id).toList());
    }

    /**
     * Exclusão em massa (categoria/tag). Ids ausentes são ignorados (idempotente); irmãos de
     * transferência são expandidos para nunca deixar uma perna órfã. Ao final, recalcula o saldo
     * uma única vez por conta distinta — sem eventos por transação individual. O SSE de conta é
     * responsabilidade de quem chama (feature), não deste use case.
     */
    public Result<Void, DomainError> deleteTransactions(List<UUID> ids) {
        val toDelete = new LinkedHashMap<UUID, Transaction>();
        for (val id : ids) {
            if (toDelete.containsKey(id)) continue;
            if (!(transactionService.findById(id) instanceof Result.Success<Transaction, DomainError>(var existing))) continue;

            toDelete.put(existing.id(), existing);
            for (val sibling : transactionService.findTransferSiblings(existing)) {
                toDelete.putIfAbsent(sibling.id(), sibling);
            }
        }

        val accountIds = new LinkedHashSet<UUID>();
        for (val tx : toDelete.values()) {
            accountIds.add(tx.accountId());
            transactionService.deleteById(tx.id());
        }

        for (val accountId : accountIds) {
            balanceRecalculationService.recalculate(accountId);
        }

        return Result.success();
    }

    public Result<Transaction, DomainError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            return Result.failure(new DomainError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val groupId = UUID.randomUUID();
        val absAmount = amount.abs();
        val outId = UUID.randomUUID();
        val inId = UUID.randomUUID();

        val outflow = new Transaction(
                outId, "Transferência (saída)", absAmount, date,
                fromAccountId, Transaction.Status.CONFIRMED, Transaction.Type.EXPENSE, CostCenter.VARIAVEL.id(), date,
                groupId, 1, 2, null, null
        );
        val inflow = new Transaction(
                inId, "Transferência (entrada)", absAmount, date,
                toAccountId, Transaction.Status.CONFIRMED, Transaction.Type.INCOME, CostCenter.VARIAVEL.id(), date,
                groupId, 2, 2, null, null
        );

        val savedOut = transactionService.save(outflow);
        val savedIn = transactionService.save(inflow);
        MessageBus.submit(new TransactionEvents.Created(savedOut));
        MessageBus.submit(new TransactionEvents.Created(savedIn));
        return Result.success(savedOut);
    }

    public Result<Transaction, DomainError> createImported(ImportedTransactionCommand cmd) {
        return validateCard(cmd.accountId(), cmd.cardId()).flatMap(ignored -> persistImported(cmd));
    }

    private Result<Transaction, DomainError> persistImported(ImportedTransactionCommand cmd) {
        val tx = new Transaction(
                UUID.randomUUID(), cmd.description(), cmd.amount().abs(), cmd.date(),
                cmd.accountId(), cmd.status(), cmd.type(), CostCenter.VARIAVEL.id(), null,
                cmd.groupId(), installmentOrDefault(cmd.installmentNumber()), installmentOrDefault(cmd.totalInstallments()), null,
                cmd.cardId());
        val saved = transactionService.save(tx);
        MessageBus.submit(new TransactionEvents.Created(saved));
        return Result.success(saved);
    }

    private Transaction toEntity(UUID id, TransactionCommand cmd, LocalDate date, Transaction.Status status,
                                 @Nullable UUID groupId, @Nullable Integer installmentNumber, @Nullable Integer totalInstallments) {
        return new Transaction(id, cmd.description(), cmd.amount().abs(), date,
                cmd.accountId(), status, cmd.type(), cmd.costCenterId(), null,
                groupId, installmentOrDefault(installmentNumber), installmentOrDefault(totalInstallments), cmd.notes(),
                cmd.cardId());
    }

    private static int installmentOrDefault(@Nullable Integer value) {
        return value == null ? 1 : value;
    }

    /** No-op when {@code cardId} is absent; otherwise the card must exist and belong to {@code accountId}. */
    private Result<Void, DomainError> validateCard(UUID accountId, @Nullable UUID cardId) {
        if (cardId == null) return Result.success();
        return cardService.findById(cardId).flatMap(card -> validateCardOwner(accountId, card));
    }

    private static Result<Void, DomainError> validateCardOwner(UUID accountId, CreditCard creditCard) {
        if (!accountId.equals(creditCard.accountId())) {
            return Result.failure(new DomainError.BusinessRule("CreditCard does not belong to account: " + creditCard.id()));
        }
        return Result.success();
    }
}
