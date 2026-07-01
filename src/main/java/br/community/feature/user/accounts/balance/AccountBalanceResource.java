package br.community.feature.user.accounts.balance;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AccountBalanceResource {

    private final MonetaryContext monetaryContext;

    @GET
    @Path("/{id}/balance")
    public Object getBalance(
            @PathParam("id") UUID id,
            @QueryParam("period") @Nullable String period,
            @QueryParam("year") @Nullable Integer year
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
