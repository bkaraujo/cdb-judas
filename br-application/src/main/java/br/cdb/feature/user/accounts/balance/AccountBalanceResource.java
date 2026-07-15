package br.cdb.feature.user.accounts.balance;

import br.cdb.feature.user.UserUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import jakarta.ws.rs.*;
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

    private final UserUseCase userUseCase;

    @GET
    @Path("/{id}/balance")
    public Object getBalance(
            @PathParam("id") UUID id,
            @QueryParam("period") @Nullable String period,
            @QueryParam("year") @Nullable Integer year
    ) {
        if (period != null) {
            val ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyyMM"));
            return switch (userUseCase.monthlyBalance(id, ym)) {
                case Result.Success(var b) -> b;
                case Result.Failure(var error) -> throw new BusinessException(error);
            };
        }
        if (year != null) {
            return switch (userUseCase.yearBalances(id, year)) {
                case Result.Success(var balances) -> balances;
                case Result.Failure(var error) -> throw new BusinessException(error);
            };
        }
        throw new BusinessException(new BusinessError.Validation("'period' or 'year' must be provided"));
    }
}
