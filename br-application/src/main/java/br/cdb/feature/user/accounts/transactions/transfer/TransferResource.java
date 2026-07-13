package br.cdb.feature.user.accounts.transactions.transfer;

import br.cdb.context.monetary.MonetaryContext;
import br.cdb.feature.user.accounts.closing.ClosingService;
import br.cdb.feature.user.accounts.core.AccountStreamPublisher;
import br.cdb.feature.user.accounts.transactions.TransactionResponse;
import br.cdb.feature.user.accounts.transactions.core.TransactionMapper;
import br.commons.Result;
import br.commons.business.BusinessException;
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
    private final AccountStreamPublisher accountStreamPublisher;

    @POST
    @Path("/transactions/transfer")
    public RestResponse<TransactionResponse> transfer(@Valid TransferRequest req) {
        if (closingService.validateDate(req.date()) instanceof Result.Failure(var error)) {
            throw new BusinessException(error);
        }
        return switch (monetaryContext.createTransfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var t) -> {
                accountStreamPublisher.upsert(req.fromAccountId());
                accountStreamPublisher.upsert(req.toAccountId());
                yield RestResponse.status(RestResponse.Status.CREATED, TransactionMapper.toDto(t, null));
            }
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
