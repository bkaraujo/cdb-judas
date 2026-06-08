package br.community.feature.user.accounts.statement;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@NullMarked
@RequiredArgsConstructor
public class StatementService {

    private final MonetaryContext monetaryContext;

    public Result<List<StatementItem>, DomainError> list(String accountId, int month, int year, @Nullable String status) {
        val uuid = UUID.fromString(accountId);

        return monetaryContext.findAccount(uuid).flatMap(account -> {
            val ym = YearMonth.of(year, month);
            val start = ym.atDay(1);
            val end = ym.atEndOfMonth();

            return monetaryContext.listTransactions().map(allTransactions -> {
                val accountTx = allTransactions.stream()
                        .filter(t -> uuid.equals(t.accountId()))
                        .toList();

                // Opening balance = initial account balance plus every transaction before this month.
                // Derived from transactions so it stays correct for any month, regardless of which
                // monthly-balance snapshots happen to be persisted.
                val openingBal = account.balance().add(
                        accountTx.stream()
                                .filter(t -> t.date().isBefore(start))
                                .map(MonetaryTransaction::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                var stream = accountTx.stream()
                        .filter(t -> !t.date().isBefore(start) && !t.date().isAfter(end));

                if (status != null && !status.isBlank()) {
                    stream = stream.filter(t -> status.equalsIgnoreCase(t.status()));
                }

                val period = stream
                        .sorted(Comparator.comparing(MonetaryTransaction::date))
                        .toList();

                val out = new ArrayList<StatementItem>();
                out.add(new StatementItem(start, "Previous balance", BigDecimal.ZERO, "balance", openingBal, null));

                BigDecimal running = openingBal;
                for (val t : period) {
                    running = running.add(t.amount());
                    out.add(new StatementItem(t.date(), t.description(), t.amount(), t.status(), running, t.categoryId()));
                }
                return out;
            });
        });
    }

    public Result<List<StatementSummary>, DomainError> summary(int month, int year, @Nullable String status) {
        val ym = YearMonth.of(year, month);
        val start = ym.atDay(1);
        val end = ym.atEndOfMonth();

        return monetaryContext.listAccounts().flatMap(accounts ->
                monetaryContext.listTransactions().map(allTransactions ->
                        accounts.stream().map(account -> {
                            var opening = account.balance();
                            var totalIn = BigDecimal.ZERO;
                            var totalOut = BigDecimal.ZERO;
                            for (val t : allTransactions) {
                                if (!account.id().equals(t.accountId())) continue;
                                if (t.date().isBefore(start)) {
                                    // carry every prior transaction forward into the opening balance
                                    opening = opening.add(t.amount());
                                    continue;
                                }
                                if (t.date().isAfter(end)) continue;
                                if (status != null && !status.isBlank() && !status.equalsIgnoreCase(t.status())) continue;
                                if (t.amount().signum() >= 0) totalIn = totalIn.add(t.amount());
                                else totalOut = totalOut.add(t.amount().abs());
                            }
                            val closing = opening.add(totalIn).subtract(totalOut);
                            return new StatementSummary(account.id(), account.name(), opening, closing, totalIn, totalOut);
                        }).toList()
                )
        );
    }
}
