package br.cdb.feature.f002._2_infrastructure.web;

import br.cdb.feature.f002._1_application.ClosingService;
import br.cdb.feature.f002._2_infrastructure.web.request.ClosingRequest;
import br.cdb.feature.f002._2_infrastructure.web.response.ClosingResponse;
import br.commons.Logger;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;

@NullMarked
@Path("/api/{uuid}/accounts/closing")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class ClosingResource {

    private final ClosingService closingService;

    @GET
    public ClosingResponse get() {
        return closingService.find()
                .map(ym -> new ClosingResponse(ym.toString()))
                .orElse(new ClosingResponse(null));
    }

    @POST
    public ClosingResponse set(@Valid ClosingRequest req) {
        Logger.debug("ClosingRequest: %s", req);
        val ym = closingService.save(YearMonth.parse(req.period()));
        return new ClosingResponse(ym.toString());
    }

    @DELETE
    public void clear() {
        Logger.debug("Clearing closing");
        closingService.clear();
    }
}
