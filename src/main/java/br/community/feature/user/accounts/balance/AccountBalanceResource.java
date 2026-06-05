package br.community.feature.user.accounts.balance;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountBalanceResource {

    private final MonetaryContext monetaryContext;

    @GetMapping("/{id}/balance")
    public Object getBalance(
            @PathVariable UUID id,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer year
    ) {
        if (period != null) {
            val ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyyMM"));
            return switch (monetaryContext.getMonthlyBalance(id, ym)) {
                case Result.Success(var b) -> b;
                case Result.Failure(var error) -> throw new DomainException(error);
            };
        }
        if (year != null) {
            return switch (monetaryContext.getYearBalances(id, year)) {
                case Result.Success(var balances) -> balances;
                case Result.Failure(var error) -> throw new DomainException(error);
            };
        }
        throw new DomainException(new DomainError.Validation("'period' or 'year' must be provided"));
    }
}
