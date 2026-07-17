package br.cdb.context.monetary._1_application.usecase;

import br.cdb.context.monetary._0_domain.event.TransactionEvents;
import br.cdb.context.monetary._0_domain.model.CostCenter;
import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.context.monetary._1_application.command.TransactionCommand;
import br.cdb.context.monetary._1_application.command.TransactionScope;
import br.cdb.context.monetary._1_application.event.TransactionEventListener;
import br.cdb.context.monetary._1_application.service.BalanceService;
import br.cdb.context.monetary._1_application.service.CreditCardService;
import br.cdb.context.monetary._1_application.service.TransactionService;
import br.commons.MessageBus;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@NullMarked
public class TransactionUseCase {

    private final TransactionService service = Registry.tryGet(TransactionService.class);
    private final BalanceService balanceService = Registry.tryGet(BalanceService.class);
    private final CreditCardService creditCardService = Registry.tryGet(CreditCardService.class);

    public TransactionUseCase() {
        MessageBus.subscribe(new TransactionEventListener());
    }


    public Result<List<Transaction>, BusinessError> transactions(UUID accountId) {
        val all = service.findByAccount(accountId).stream()
                .sorted(Comparator.comparing(Transaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    public Result<List<Transaction>, BusinessError> transactions() {
        val all = service.findAll().stream()
                .sorted(Comparator.comparing(Transaction::date).reversed())
                .toList();
        return Result.success(all);
    }

    public Result<List<Transaction>, BusinessError> pending() {
        return Result.success(service.findPending());
    }

    public Result<Transaction, BusinessError> transaction(UUID id) {
        return service.findById(id);
    }

    public Result<Transaction, BusinessError> upsert(TransactionCommand.Upsert cmd) {
        return switch (cmd) {
            case TransactionCommand.Create create ->
                    validateCard(create.accountId(), create.cardId()).flatMap(ignored -> dispatchCreate(create));
            case TransactionCommand.Update update ->
                    transaction(update.id()).flatMap(existing -> dispatchUpdate(update.id(), existing, update));
        };
    }

    private Result<Transaction, BusinessError> dispatchCreate(TransactionCommand.Create cmd) {
        val count = installmentCount(cmd);
        return count == 1 ? createSingle(cmd) : createInstallments(cmd, count);
    }

    private static int installmentCount(TransactionCommand.Create cmd) {
        return cmd.installments() != null && cmd.installments() > 1 ? cmd.installments() : 1;
    }

    private Result<Transaction, BusinessError> createSingle(TransactionCommand.Create cmd) {
        val saved = service.save(toEntity(UUID.randomUUID(), cmd.description(), cmd.amount(), cmd.date(),
                cmd.accountId(), cmd.status(), cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(), null, null, null));
        MessageBus.submit(new TransactionEvents.Created(saved));
        return Result.success(saved);
    }

    private Result<Transaction, BusinessError> createInstallments(TransactionCommand.Create cmd, int installmentsCount) {
        val groupId = UUID.randomUUID();
        val batch = new ArrayList<Transaction>();

        for (int i = 1; i <= installmentsCount; i++) {
            val date = cmd.date().plusMonths(i - 1);
            val status = (i == 1) ? cmd.status() : Transaction.Status.PENDING;
            batch.add(toEntity(UUID.randomUUID(), cmd.description(), cmd.amount(), date, cmd.accountId(), status,
                    cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(), groupId, i, installmentsCount));
        }

        Transaction first = null;
        for (val t : batch) {
            val saved = service.save(t);
            if (first == null) first = saved;
        }

        if (first != null) MessageBus.submit(new TransactionEvents.Created(first));

        return Result.success(first);
    }

    private Result<Transaction, BusinessError> dispatchUpdate(UUID id, Transaction existing, TransactionCommand.Update cmd) {
        val transferSiblings = service.findTransferSiblings(existing);
        if (!transferSiblings.isEmpty()) return updateTransfer(existing, transferSiblings, cmd);
        return validateCard(cmd.accountId(), cmd.cardId()).flatMap(ignored -> updateNonTransfer(id, existing, cmd));
    }

    private Result<Transaction, BusinessError> updateNonTransfer(UUID id, Transaction existing, TransactionCommand.Update cmd) {
        val isFuture = cmd.scope() instanceof TransactionScope.Future && existing.groupId() != null;

        if (!isFuture) {
            val updated = service.save(toEntity(id, cmd.description(), cmd.amount(), cmd.date(), cmd.accountId(),
                    cmd.status(), cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(),
                    existing.groupId(), existing.installmentNumber(), existing.totalInstallments()));
            MessageBus.submit(new TransactionEvents.Updated(existing));
            MessageBus.submit(new TransactionEvents.Updated(updated));
            return Result.<Transaction, BusinessError>success(updated);
        }

        val groupId = existing.groupId();
        val installmentNumber = existing.installmentNumber();
        if (groupId == null) return Result.<Transaction, BusinessError>success(existing);

        val all = service.findByGroupId(groupId).stream()
                .filter(t -> t.installmentNumber() >= installmentNumber)
                .sorted(Comparator.comparing(Transaction::installmentNumber))
                .toList();

        Transaction firstSaved = null;
        for (val t : all) {
            val currentNumber = t.installmentNumber();
            val newDate = cmd.date().plusMonths(currentNumber - installmentNumber);
            val updated = service.save(toEntity(t.id(), cmd.description(), cmd.amount(), newDate, cmd.accountId(),
                    t.status(), cmd.type(), cmd.costCenterId(), cmd.notes(), cmd.cardId(),
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
    private Result<Transaction, BusinessError> updateTransfer(Transaction edited, List<Transaction> siblings, TransactionCommand.Update cmd) {
        val newAccount = cmd.accountId();
        if (siblings.stream().anyMatch(sib -> newAccount.equals(sib.accountId()))) {
            return Result.failure(new BusinessError.BusinessRule("Conta de origem e destino devem ser diferentes"));
        }

        val absAmount = cmd.amount().abs();
        val updatedEdited = service.save(withTransferEdits(edited, newAccount, absAmount, cmd.date(), cmd.status()));
        MessageBus.submit(new TransactionEvents.Updated(edited));
        MessageBus.submit(new TransactionEvents.Updated(updatedEdited));

        for (val sib : siblings) {
            val updatedSib = service.save(withTransferEdits(sib, sib.accountId(), absAmount, cmd.date(), cmd.status()));
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

    public Result<Transaction, BusinessError> updateTransactionStatus(UUID id, Transaction.Status status, @Nullable LocalDate paymentDate) {
        return transaction(id)
                .map(existing -> {
                    val saved = service.save(new Transaction(
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
    public Result<List<UUID>, BusinessError> delete(TransactionCommand.Delete command) {
        return transaction(command.id()).flatMap(existing -> {
            val transferSiblings = service.findTransferSiblings(existing);
            if (!transferSiblings.isEmpty()) return deleteTransferGroup(existing, transferSiblings);

            val isFuture = command.scope() instanceof TransactionScope.Future && existing.groupId() != null;
            val groupId = existing.groupId();

            if (!isFuture || groupId == null) {
                return service.deleteById(command.id()).map(ignored -> {
                    MessageBus.submit(new TransactionEvents.Deleted(existing));
                    return List.of(command.id());
                });
            }

            val installmentNumber = existing.installmentNumber();
            val toDelete = service.findByGroupId(groupId).stream()
                    .filter(t -> t.installmentNumber() >= installmentNumber)
                    .toList();

            for (val t : toDelete) {
                val delRes = service.deleteById(t.id());
                if (delRes instanceof Result.Failure<Void, BusinessError>(BusinessError error)) return Result.<List<UUID>>failure(error);
            }

            MessageBus.submit(new TransactionEvents.Deleted(existing));
            return Result.success(toDelete.stream().map(Transaction::id).toList());
        });
    }

    private Result<List<UUID>, BusinessError> deleteTransferGroup(Transaction leg, List<Transaction> siblings) {
        val legs = new ArrayList<Transaction>();
        legs.add(leg);
        legs.addAll(siblings);
        for (val t : legs) {
            if (service.deleteById(t.id()) instanceof Result.Failure<Void, BusinessError>(BusinessError error)) {
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
    public Result<Void, BusinessError> deleteTransactions(List<UUID> ids) {
        val toDelete = new LinkedHashMap<UUID, Transaction>();
        for (val id : ids) {
            if (toDelete.containsKey(id)) continue;
            if (!(transaction(id) instanceof Result.Success<Transaction, BusinessError>(var existing))) continue;

            toDelete.put(existing.id(), existing);
            for (val sibling : service.findTransferSiblings(existing)) {
                toDelete.putIfAbsent(sibling.id(), sibling);
            }
        }

        val accountIds = new LinkedHashSet<UUID>();
        for (val tx : toDelete.values()) {
            accountIds.add(tx.accountId());
            service.deleteById(tx.id());
        }

        for (val accountId : accountIds) {
            balanceService.recalculate(accountId);
        }

        return Result.success();
    }

    public Result<Transaction, BusinessError> createTransfer(UUID fromAccountId, UUID toAccountId, LocalDate date, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            return Result.failure(new BusinessError.BusinessRule("Conta de origem e destino devem ser diferentes"));
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

        val savedOut = service.save(outflow);
        val savedIn = service.save(inflow);
        MessageBus.submit(new TransactionEvents.Created(savedOut));
        MessageBus.submit(new TransactionEvents.Created(savedIn));
        return Result.success(savedOut);
    }

    /** Persiste uma transação já montada pelo chamador. Valida a invariante de cartão. */
    public Result<Transaction, BusinessError> create(Transaction tx) {
        return validateCard(tx.accountId(), tx.cardId()).flatMap(ignored -> {
            val saved = service.save(tx);
            MessageBus.submit(new TransactionEvents.Created(saved));
            return Result.success(saved);
        });
    }

    private Transaction toEntity(UUID id, String description, BigDecimal amount, LocalDate date, UUID accountId,
                                 Transaction.Status status, Transaction.Type type, UUID costCenterId,
                                 @Nullable String notes, @Nullable UUID cardId,
                                 @Nullable UUID groupId, @Nullable Integer installmentNumber, @Nullable Integer totalInstallments) {
        return new Transaction(id, description, amount.abs(), date,
                accountId, status, type, costCenterId, null,
                groupId, installmentOrDefault(installmentNumber), installmentOrDefault(totalInstallments), notes,
                cardId);
    }

    private static int installmentOrDefault(@Nullable Integer value) {
        return value == null ? 1 : value;
    }

    /** No-op when {@code cardId} is absent; otherwise the card must exist and belong to {@code accountId}. */
    private Result<Void, BusinessError> validateCard(UUID accountId, @Nullable UUID cardId) {
        if (cardId == null) return Result.success();
        return creditCardService.findById(cardId).flatMap(card -> validateCardOwner(accountId, card));
    }

    private static Result<Void, BusinessError> validateCardOwner(UUID accountId, CreditCard creditCard) {
        if (!accountId.equals(creditCard.accountId())) {
            return Result.failure(new BusinessError.BusinessRule("CreditCard does not belong to account: " + creditCard.id()));
        }
        return Result.success();
    }
}
