package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.AccountBalance;
import br.commons.Logger;
import br.commons.Registry;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Recalcula os snapshots mensais de saldo de uma conta a partir de suas transações atuais. */
@NullMarked
public class BalanceRecalculationService {

    private final AccountService accountService = Registry.tryGet(AccountService.class);
    private final BalanceService balanceService = Registry.tryGet(BalanceService.class);
    private final TransactionService transactionService = Registry.tryGet(TransactionService.class);

    @NullMarked
    private record Balance (
            LocalDate date,
            BigDecimal amount
    ) {}

    public void recalculate(UUID accountId) {
        val accountResult = accountService.findById(accountId);
        if (accountResult.isFailure()) return;
        val transactions = transactionService.findByAccount(accountId).stream()
                .map(t -> new Balance(t.date(), BigDecimal.valueOf(t.signal()).multiply(t.amount())))
                .toList();

        recalculateBalance(accountId, BigDecimal.ZERO, transactions);
    }

    private void recalculateBalance(UUID accountId, BigDecimal initialBalance, List<Balance> transactions) {
        val existingBalances = balanceService.findByAccount(accountId);

        if (transactions.isEmpty()) {
            // No activity left → no monthly snapshots make sense; drop any stale rows so reads
            // fall back to the account's initial balance.
            existingBalances.forEach(balanceService::deleteById);
            return;
        }

        val firstMonth = transactions.stream()
                .map(t -> YearMonth.from(t.date()))
                .min(Comparator.naturalOrder())
                .orElse(YearMonth.now(ZoneId.systemDefault()));

        // Recompute the whole timeline from the first activity through the current month, so every
        // month up to today has a snapshot (months with no movement carry the prior balance forward).
        var endMonth = YearMonth.now(ZoneId.systemDefault());
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
                .forEach(balanceService::deleteById);

        Logger.verbose("Recalculating balance for account %s from %s to %s", accountId, firstMonth, endMonth);

        var runningBalance = initialBalance;
        var current = firstMonth;
        while (!current.isAfter(endMonth)) {
            val finalCurrent = current;
            val monthSum = transactions.stream()
                    .filter(t -> YearMonth.from(t.date()).equals(finalCurrent))
                    .map(Balance::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            runningBalance = runningBalance.add(monthSum);
            upsertBalance(accountId, current, runningBalance, existingBalances);
            current = current.plusMonths(1);
        }
    }

    private void upsertBalance(UUID accountId, YearMonth period, BigDecimal balance, List<AccountBalance> existing) {
        val existingOpt = existing.stream().filter(b -> b.period().equals(period)).findFirst();
        if (existingOpt.isPresent()) {
            val b = existingOpt.get();
            if (b.balance().compareTo(balance) != 0) {
                balanceService.save(new AccountBalance(accountId, period, balance));
            }
        } else {
            balanceService.save(new AccountBalance(accountId, period, balance));
        }
    }
}
