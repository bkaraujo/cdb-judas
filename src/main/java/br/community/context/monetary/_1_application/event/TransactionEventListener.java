package br.community.context.monetary._1_application.event;

import br.commons.Logger;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import br.community.context.monetary._0_domain.event.MonetaryEvent;
import br.community.context.monetary._0_domain.model.MonetaryBalance;
import br.community.context.monetary._0_domain.model.MonthlyBalance;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.BalanceService;
import br.community.context.monetary._1_application.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@NullMarked
public class TransactionEventListener {

    private final AccountService accountService;
    private final BalanceService balanceService;
    private final TransactionService transactionService;

    @MessageListener
    public MessageResult onTransaction(MonetaryEvent.TransactionCreated transaction) {
        triggerRecalculate(transaction.transaction().accountId());
        return MessageResult.AVAILABLE;
    }

    @MessageListener
    public MessageResult onTransaction(MonetaryEvent.TransactionUpdated transaction) {
        triggerRecalculate(transaction.transaction().accountId());
        return MessageResult.AVAILABLE;
    }

    @MessageListener
    public MessageResult onTransaction(MonetaryEvent.TransactionDeleted transaction) {
        triggerRecalculate(transaction.transaction().accountId());
        return MessageResult.AVAILABLE;
    }

    @MessageListener
    public MessageResult onAccountDeleted(MonetaryEvent.AccountDeleted event) {
        val accountId = event.accountId();

        transactionService.findByAccount(accountId)
                .forEach(t -> transactionService.deleteById(t.id()));

        accountService.findAll().stream()
                .filter(a -> accountId.equals(a.linkedAccountId()))
                .forEach(a -> accountService.deleteById(a.id()));

        balanceService.findByAccount(accountId)
                .forEach(b -> balanceService.deleteById(b.id()));

        return MessageResult.AVAILABLE;
    }

    private void triggerRecalculate(UUID accountId) {
        val accountResult = accountService.findById(accountId);
        if (accountResult.isFailure()) return;
        val initialBalance = accountResult.getOrThrow().balance();
        val transactions = transactionService.findByAccount(accountId).stream()
                .map(t -> new MonetaryBalance(t.date(), t.amount()))
                .toList();

        recalculateBalance(accountId, initialBalance, transactions);
    }

    private void recalculateBalance(UUID accountId, BigDecimal initialBalance, List<MonetaryBalance> transactions) {
        val existingBalances = balanceService.findByAccount(accountId);

        if (transactions.isEmpty()) {
            // No activity left → no monthly snapshots make sense; drop any stale rows so reads
            // fall back to the account's initial balance.
            existingBalances.forEach(b -> balanceService.deleteById(b.id()));
            return;
        }

        val firstMonth = transactions.stream()
                .map(t -> YearMonth.from(t.date()))
                .min(Comparator.naturalOrder())
                .orElse(YearMonth.now());

        // Recompute the whole timeline from the first activity through the current month, so every
        // month up to today has a snapshot (months with no movement carry the prior balance forward).
        var endMonth = YearMonth.now();
        for (val t : transactions) {
            val month = YearMonth.from(t.date());
            if (month.isAfter(endMonth)) endMonth = month;
        }
        for (val b : existingBalances) {
            if (b.period().isAfter(endMonth)) endMonth = b.period();
        }

        // Drop snapshots that precede all current activity (e.g. the earliest transactions were deleted).
        existingBalances.stream()
                .filter(b -> b.period().isBefore(firstMonth))
                .forEach(b -> balanceService.deleteById(b.id()));

        Logger.verbose("Recalculating balance for account %s from %s to %s", accountId, firstMonth, endMonth);

        var runningBalance = initialBalance;
        var current = firstMonth;
        while (!current.isAfter(endMonth)) {
            val finalCurrent = current;
            val monthSum = transactions.stream()
                    .filter(t -> YearMonth.from(t.date()).equals(finalCurrent))
                    .map(MonetaryBalance::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            runningBalance = runningBalance.add(monthSum);
            upsertBalance(accountId, current, runningBalance, existingBalances);
            current = current.plusMonths(1);
        }
    }

    private void upsertBalance(UUID accountId, YearMonth period, BigDecimal balance, List<MonthlyBalance> existing) {
        val existingOpt = existing.stream().filter(b -> b.period().equals(period)).findFirst();
        if (existingOpt.isPresent()) {
            val b = existingOpt.get();
            if (b.balance().compareTo(balance) != 0) {
                balanceService.save(new MonthlyBalance(b.id(), accountId, period, balance));
            }
        } else {
            balanceService.save(new MonthlyBalance(UUID.randomUUID(), accountId, period, balance));
        }
    }
}
