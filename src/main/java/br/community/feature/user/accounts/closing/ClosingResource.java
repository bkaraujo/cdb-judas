package br.community.feature.user.accounts.closing;

import br.commons.Logger;
import br.community.context.monetary.MonetaryContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/accounts/closing", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClosingResource {

    private final MonetaryContext monetaryContext;

    @GetMapping
    public ClosingResponse get() {
        return monetaryContext.getClosingPeriod()
                .map(ym -> new ClosingResponse(ym.toString()))
                .orElse(new ClosingResponse(null));
    }

    @PostMapping
    public ClosingResponse set(@RequestBody @Valid ClosingRequest req) {
        Logger.debug("ClosingRequest: %s", req);
        val ym = monetaryContext.setClosingPeriod(YearMonth.parse(req.period()));
        return new ClosingResponse(ym.toString());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        Logger.debug("Clearing closing");
        monetaryContext.clearClosingPeriod();
    }
}
