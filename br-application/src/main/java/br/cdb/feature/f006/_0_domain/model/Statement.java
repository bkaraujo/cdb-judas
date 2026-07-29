package br.cdb.feature.f006._0_domain.model;

import br.cdb.feature.f002._0_domain.model.Account;
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
