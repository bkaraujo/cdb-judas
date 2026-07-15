package br.cdb.feature.user.accounts.transactions.transfer;

import br.cdb.feature.user.UserUseCase;
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

    private final UserUseCase userUseCase;

    @POST
    @Path("/transactions/transfer")
    public RestResponse<TransactionResponse> transfer(@Valid TransferRequest req) {
        return switch (userUseCase.transfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var view) ->
                    RestResponse.status(RestResponse.Status.CREATED, TransactionMapper.toDto(view.transaction(), view.overlay()));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
