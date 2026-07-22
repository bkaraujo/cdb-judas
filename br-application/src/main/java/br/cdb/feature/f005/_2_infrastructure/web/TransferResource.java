package br.cdb.feature.f005._2_infrastructure.web;

import br.cdb.feature.f005._1_application.TransactionUseCase;
import br.cdb.feature.f005._2_infrastructure.web.request.TransferRequest;
import br.cdb.feature.f005._2_infrastructure.web.response.TransactionResponse;
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

    private final TransactionUseCase transactionUseCase;

    @POST
    @Path("/transactions/transfer")
    public RestResponse<TransactionResponse> transfer(@Valid TransferRequest req) {
        return switch (transactionUseCase.transfer(req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var view) ->
                    RestResponse.status(RestResponse.Status.CREATED, TransactionMapper.toDto(view.transaction(), view.overlay()));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
