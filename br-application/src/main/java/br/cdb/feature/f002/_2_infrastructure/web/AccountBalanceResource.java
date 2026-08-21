package br.cdb.feature.f002._2_infrastructure.web;

import br.cdb.feature.f002._1_application.usecase.ReadUseCase;
import br.cdb.feature.f002._2_infrastructure.web.response.BalanceResponse;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountBalanceResource {

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    /** Saldo do período de todas as contas do usuário numa só resposta — evita N chamadas
     *  por conta no frontend (usado pela tela de Extrato de Contas). */
    @GET
    @Path("/balance")
    public List<BalanceResponse> listBalances(@QueryParam("period") String period) {
        if (period == null) {
            throw new BusinessException(new BusinessError.Validation("f002.balance.periodRequired"));
        }
        val ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyyMM"));
        return switch (reads.balances(ym)) {
            case Result.Success(var balances) -> balances.stream().map(BalanceResponse::of).toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @GET
    @Path("/{id}/balance")
    public Object getBalance(
            @PathParam("id") UUID id,
            @QueryParam("period") @Nullable String period,
            @QueryParam("year") @Nullable Integer year
    ) {
        if (period != null) {
            val ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyyMM"));
            return switch (reads.monthlyBalance(id, ym)) {
                case Result.Success(var b) -> BalanceResponse.of(b);
                case Result.Failure(var error) -> throw new BusinessException(error);
            };
        }
        if (year != null) {
            return switch (reads.yearBalances(id, year)) {
                case Result.Success(var balances) -> balances.stream().map(BalanceResponse::of).toList();
                case Result.Failure(var error) -> throw new BusinessException(error);
            };
        }
        throw new BusinessException(new BusinessError.Validation("f002.balance.periodOrYearRequired"));
    }
}
