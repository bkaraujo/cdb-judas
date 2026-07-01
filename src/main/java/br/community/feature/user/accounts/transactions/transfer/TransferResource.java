package br.community.feature.user.accounts.transactions.transfer;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.closing.ClosingService;
import br.community.feature.user.accounts.transactions.TransactionResponse;
import br.community.feature.user.accounts.transactions.core.TransactionMapper;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TransferResource {

    private final MonetaryContext monetaryContext;
    private final ClosingService closingService;

    @POST
    @Path("/transactions/transfer")
    public RestResponse<TransactionResponse> transfer(@Valid TransferRequest req) {
        if (closingService.validateDate(req.date()) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
        return switch (monetaryContext.createTransfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var t) -> RestResponse.status(RestResponse.Status.CREATED, TransactionMapper.toDto(t, null));
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }
}
