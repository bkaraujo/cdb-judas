package br.community.context.monetary._0_domain.model;

import org.jspecify.annotations.NullMarked;
import java.time.YearMonth;
import java.util.List;

@NullMarked
public record Statement(
        Account account,
        YearMonth yearMonth,
        List<Transaction> transactions
) {
}
