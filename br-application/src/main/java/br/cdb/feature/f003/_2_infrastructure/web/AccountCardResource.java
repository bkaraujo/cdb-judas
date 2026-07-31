package br.cdb.feature.f003._2_infrastructure.web;

import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f003._2_infrastructure.web.response.CardResponse;
import br.cdb.feature.f003._1_application.command.CreditCardCommand;
import br.cdb.feature.f003._1_application.usecase.ReadUseCase;
import br.cdb.feature.f003._1_application.usecase.WriteUseCase;
import br.cdb.feature.f003._2_infrastructure.web.request.CardRequest;
import br.cdb.feature.f003._2_infrastructure.web.request.CardStatusRequest;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts/{accountId}/cards")
@Produces(MediaType.APPLICATION_JSON)
public class AccountCardResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);
    private final WriteUseCase writes = Context.tryGet(WriteUseCase.class);

    @GET
    public List<CardResponse> list(@PathParam("accountId") UUID accountId) {
        return switch (reads.cards(accountId)) {
            case Result.Success(var cards) -> cards.stream().map(CardResponse::from).toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @POST
    public RestResponse<CardResponse> create(@PathParam("accountId") UUID accountId, @Valid CardRequest req) {
        return switch (writes.createCard(new CreditCardCommand.Create(accountId, req.last4()))) {
            case Result.Success(var card) -> RestResponse.status(RestResponse.Status.CREATED, CardResponse.from(card));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @DELETE
    @Path("/{cardId}")
    public Response delete(
            @PathParam("accountId") UUID accountId,
            @PathParam("cardId") UUID cardId,
            @QueryParam("strategy") @Nullable String strategy,
            @QueryParam("targetId") @Nullable UUID targetId
    ) {
        return Deletions.execute(strategy, targetId, ALLOWED_STRATEGIES,
                parsed -> writes.deleteCard(accountId, cardId, parsed, targetId),
                "a este cartão.");
    }

    @PATCH
    @Path("/{cardId}")
    public CardResponse updateStatus(@PathParam("accountId") UUID accountId, @PathParam("cardId") UUID cardId, @Valid CardStatusRequest req) {
        return switch (writes.setCardActive(accountId, cardId, req.active())) {
            case Result.Success(var card) -> CardResponse.from(card);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
