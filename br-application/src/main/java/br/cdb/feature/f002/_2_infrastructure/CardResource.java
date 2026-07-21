package br.cdb.feature.f002._2_infrastructure;

import br.cdb.context.monetary._1_application.command.CreditCardCommand;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f002._1_application.AccountUseCase;
import br.cdb.feature.f002._1_application.CardResponse;
import br.commons.Result;
import br.commons.business.BusinessException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts/{accountId}/cards")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CardResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final AccountUseCase accountUseCase;

    @GET
    public List<CardResponse> list(@PathParam("accountId") UUID accountId) {
        return switch (accountUseCase.cards(accountId)) {
            case Result.Success(var cards) -> cards.stream().map(CardResponse::from).toList();
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @POST
    public RestResponse<CardResponse> create(@PathParam("accountId") UUID accountId, @Valid CardRequest req) {
        return switch (accountUseCase.createCard(new CreditCardCommand.Create(accountId, req.last4()))) {
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
                parsed -> accountUseCase.deleteCard(accountId, cardId, parsed, targetId),
                "a este cartão.");
    }

    @PATCH
    @Path("/{cardId}")
    public CardResponse updateStatus(@PathParam("accountId") UUID accountId, @PathParam("cardId") UUID cardId, @Valid CardStatusRequest req) {
        return switch (accountUseCase.setCardActive(accountId, cardId, req.active())) {
            case Result.Success(var card) -> CardResponse.from(card);
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }
}
