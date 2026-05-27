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
import java.time.LocalDate;
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
        triggerRecalculate(transaction.transaction().accountId(), transaction.transaction().date());
        return MessageResult.AVAILABLE;
    }

    @MessageListener
    public MessageResult onTransaction(MonetaryEvent.TransactionUpdated transaction) {
        triggerRecalculate(transaction.transaction().accountId(), transaction.transaction().date());
        return MessageResult.AVAILABLE;
    }

    @MessageListener
    public MessageResult onTransaction(MonetaryEvent.TransactionDeleted transaction) {
        triggerRecalculate(transaction.transaction().accountId(), transaction.transaction().date());
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

    private void triggerRecalculate(UUID accountId, LocalDate referenceDate) {
        val accountResult = accountService.findById(accountId);
        if (accountResult.isFailure()) return;
        val initialBalance = accountResult.getOrThrow().balance();
        val summaries = transactionService.findByAccount(accountId).stream()
                .map(t -> new MonetaryBalance(t.date(), t.amount()))
                .toList();

        recalculateBalance(accountId, initialBalance, YearMonth.from(referenceDate), summaries);
    }

    private void recalculateBalance(UUID accountId, BigDecimal initialBalance, YearMonth start, List<MonetaryBalance> monetaryTransactions) {
        Logger.verbose("Recalculating balance for account %s at %s", accountId, start);
        val existingBalances = balanceService.findByAccount(accountId);

        val lastMonthWithBalance = existingBalances.stream()
                .map(MonthlyBalance::period)
                .max(Comparator.naturalOrder())
                .orElse(start);

        val lastMonthWithMonetaryTransaction = monetaryTransactions.stream()
                .map(t -> YearMonth.from(t.date()))
                .max(Comparator.naturalOrder())
                .orElse(start);

        val endMonth = lastMonthWithBalance.isAfter(lastMonthWithMonetaryTransaction) ? lastMonthWithBalance : lastMonthWithMonetaryTransaction;

        var runningBalance = initialBalance.add(
                monetaryTransactions.stream()
                        .filter(t -> YearMonth.from(t.date()).isBefore(start))
                        .map(MonetaryBalance::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        var current = start;
        while (!current.isAfter(endMonth)) {
            val finalCurrent = current;
            val monthSum = monetaryTransactions.stream()
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
