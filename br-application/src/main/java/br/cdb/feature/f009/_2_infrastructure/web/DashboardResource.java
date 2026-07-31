package br.cdb.feature.f009._2_infrastructure.web;

import br.cdb.feature.f009._1_application.usecase.ReadUseCase;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Path("/api/{uuid}/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);

    @GET
    public ReadUseCase.MonthlyResult getMonthlyResult(
            @QueryParam("month") int month,
            @QueryParam("year") int year
    ) {
        return switch (reads.getMonthlyResult(month, year)) {
            case Result.Success(var data) -> data;
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
