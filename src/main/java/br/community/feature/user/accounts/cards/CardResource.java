package br.community.feature.user.accounts.cards;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._1_application.command.CardCommand;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.accounts.transactions.UserTransactionService;
import br.community.feature.user.deletion.DeletionStrategy;
import br.community.feature.user.deletion.Deletions;
import br.community.feature.user.tags.UserTransactionTagService;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.val;
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

    private final MonetaryContext monetaryContext;
    private final UserTransactionService userTransactionService;
    private final UserTransactionTagService tagLinkService;

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
    public Response delete(
            @PathParam("accountId") UUID accountId,
            @PathParam("cardId") UUID cardId,
            @QueryParam("strategy") @Nullable String strategy,
            @QueryParam("targetId") @Nullable UUID targetId
    ) {
        guardBelongsToAccount(accountId, cardId);

        val parsed = Deletions.parse(strategy, targetId, ALLOWED_STRATEGIES);
        if (parsed instanceof Result.Failure<DeletionStrategy, DomainError>(var error)) {
            throw new DomainException(error);
        }
        val deletionStrategy = parsed.get();

        if (deletionStrategy == null) {
            val count = countLinkedTransactions(cardId);
            if (count > 0) {
                return Deletions.linkedConflict(null, count, "Existem " + count + " transações vinculadas a este cartão.");
            }
        }

        val policy = Deletions.toPolicy(deletionStrategy, targetId);
        return switch (monetaryContext.deleteCard(cardId, policy)) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var ids) -> {
                // MOVE mantém o cartão de destino na mesma conta: sem re-key de overlay a fazer.
                if (deletionStrategy != DeletionStrategy.MOVE) {
                    ids.forEach(id -> {
                        userTransactionService.deleteByTransaction(id);
                        tagLinkService.deleteByTransaction(id);
                    });
                }
                yield Response.noContent().build();
            }
        };
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

    private int countLinkedTransactions(UUID cardId) {
        return switch (monetaryContext.listTransactions()) {
            case Result.Success(var all) -> (int) all.stream().filter(t -> cardId.equals(t.cardId())).count();
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
