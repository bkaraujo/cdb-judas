package br.cdb.feature.f006._2_infrastructure.web;

import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.cdb.feature.f006._2_infrastructure.web.request.TransferRequest;
import br.cdb.feature.f006._2_infrastructure.web.response.TransactionResponse;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class TransferResource {

    private final WriteUseCases writes = Context.tryGet(WriteUseCases.class);

    @POST
    @Path("/transactions/transfer")
    public RestResponse<TransactionResponse> transfer(@Valid TransferRequest req) {
        val personId = UUID.fromString(HTTPRequest.personId());
        return switch (writes.transfer(personId, req.fromAccountId(), req.toAccountId(), req.date(), req.amount())) {
            case Result.Success(var transaction) ->
                    RestResponse.status(RestResponse.Status.CREATED, RequestMapper.toDto(transaction));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
