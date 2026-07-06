package br.community.feature.user.accounts.cards;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/accounts/{accountId}/cards")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CardResource {

    private final MonetaryContext monetaryContext;

    @GET
    public List<CardResponse> list(@PathParam("accountId") UUID accountId) {
        return switch (monetaryContext.listCardsByAccount(accountId)) {
            case Result.Success(var cards) -> cards.stream().map(CardResponse::from).toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @POST
    public RestResponse<CardResponse> create(@PathParam("accountId") UUID accountId, @Valid CardRequest req) {
        return switch (monetaryContext.createCard(new CardCommand(accountId, req.last4()))) {
            case Result.Success(var card) -> RestResponse.status(RestResponse.Status.CREATED, CardResponse.from(card));
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DELETE
    @Path("/{cardId}")
    public void delete(@PathParam("accountId") UUID accountId, @PathParam("cardId") UUID cardId) {
        guardBelongsToAccount(accountId, cardId);
        if (monetaryContext.deleteCard(cardId) instanceof Result.Failure(var error)) {
            throw new DomainException(error);
        }
    }

    @PATCH
    @Path("/{cardId}")
    public CardResponse updateStatus(@PathParam("accountId") UUID accountId, @PathParam("cardId") UUID cardId, @Valid CardStatusRequest req) {
        guardBelongsToAccount(accountId, cardId);
        return switch (monetaryContext.setCardActive(cardId, req.active())) {
            case Result.Success(var card) -> CardResponse.from(card);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    private void guardBelongsToAccount(UUID accountId, UUID cardId) {
        val owned = switch (monetaryContext.listCardsByAccount(accountId)) {
            case Result.Success(var cards) -> cards.stream().anyMatch(c -> c.id().equals(cardId));
            case Result.Failure(var error) -> throw new DomainException(error);
        };
        if (!owned) throw new DomainException(new DomainError.NotFound("Card not found: " + cardId));
    }
}
