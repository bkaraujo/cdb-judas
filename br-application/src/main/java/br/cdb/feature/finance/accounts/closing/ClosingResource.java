package br.cdb.feature.finance.accounts.closing;

import br.cdb.feature.user.UserUseCase;
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

    private final UserUseCase userUseCase;

    @GET
    public ClosingResponse get() {
        return userUseCase.closing()
                .map(ym -> new ClosingResponse(ym.toString()))
                .orElse(new ClosingResponse(null));
    }

    @POST
    public ClosingResponse set(@Valid ClosingRequest req) {
        Logger.debug("ClosingRequest: %s", req);
        val ym = userUseCase.saveClosing(YearMonth.parse(req.period()));
        return new ClosingResponse(ym.toString());
    }

    @DELETE
    public void clear() {
        Logger.debug("Clearing closing");
        userUseCase.clearClosing();
    }
}
