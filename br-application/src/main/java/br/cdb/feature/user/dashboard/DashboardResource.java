package br.cdb.feature.user.dashboard;

import br.cdb.feature.user.UserUseCase;
import br.cdb.feature.user.dashboard.core.DashboardService;
import br.commons.Result;
import br.commons.business.BusinessException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Path("/api/{uuid}/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class DashboardResource {

    private final UserUseCase userUseCase;

    @GET
    @Path("/result")
    public DashboardService.MonthlyResult getMonthlyResult(
            @QueryParam("month") int month,
            @QueryParam("year") int year) {
        return switch (userUseCase.monthlyResult(month, year)) {
            case Result.Success(var data) -> data;
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
