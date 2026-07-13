package br.cdb.feature.user.dashboard;

import br.cdb.feature.user.dashboard.core.DashboardService;
import br.commons.Result;
import br.commons.business.BusinessException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Path("/api/{uuid}/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class DashboardResource {

    private final DashboardService service;

    @GET
    @Path("/result")
    public DashboardService.MonthlyResult getMonthlyResult(
            @QueryParam("month") int month,
            @QueryParam("year") int year) {
        val result = service.getMonthlyResult(month, year);
        return switch (result) {
            case Result.Success(var data) -> data;
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
