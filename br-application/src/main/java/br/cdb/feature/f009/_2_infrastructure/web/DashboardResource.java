package br.cdb.feature.f009._2_infrastructure.web;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f009._1_application.DashboardService;
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

    private final DashboardService dashboardService;

    @GET
    public DashboardService.MonthlyResult getMonthlyResult(
            @QueryParam("month") int month,
            @QueryParam("year") int year
    ) {
        return switch (dashboardService.getMonthlyResult(HTTPRequest.personId(), month, year)) {
            case Result.Success(var data) -> data;
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
