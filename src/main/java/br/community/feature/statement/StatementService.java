package br.community.feature.statement;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
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

            val openingBal = monetaryContext.getMonthlyBalance(uuid, ym.minusMonths(1))
                    .map(b -> b.balance())
                    .getOrElse(account.balance());

            return monetaryContext.listTransactions().map(allTransactions -> {
                var stream = allTransactions.stream()
                        .filter(t -> uuid.equals(t.accountId()))
                        .filter(t -> !t.date().isBefore(start) && !t.date().isAfter(end));

                if (status != null && !status.isBlank()) {
                    stream = stream.filter(t -> status.equalsIgnoreCase(t.status()));
                }

                val period = stream
                        .sorted(Comparator.comparing(br.community.context.monetary._0_domain.model.MonetaryTransaction::date))
                        .toList();

                val out = new ArrayList<StatementItem>();
                out.add(new StatementItem(start, "Previous balance", BigDecimal.ZERO, "balance", openingBal));

                BigDecimal running = openingBal;
                for (val t : period) {
                    running = running.add(t.amount());
                    out.add(new StatementItem(t.date(), t.description(), t.amount(), t.status(), running));
                }
                return out;
            });
        });
    }
}
