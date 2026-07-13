package br.cdb.feature.user.tags;

import br.cdb.feature.user.deletion.DeletionStrategy;
import br.cdb.feature.user.deletion.Deletions;
import br.cdb.feature.user.tags.core.TagRequest;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.business.BusinessException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/tags")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TagResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES =
            Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE, DeletionStrategy.DETACH);

    private final UserTagService userTagService;

    @GET
    public List<UserTag> listAll(@PathParam("uuid") UUID uuid) {
        return userTagService.findAll(uuid);
    }

    @POST
    public RestResponse<UserTag> create(@PathParam("uuid") UUID uuid, @Valid TagRequest req) {
        return RestResponse.status(RestResponse.Status.CREATED, userTagService.create(uuid, req.name(), req.color()));
    }

    @PATCH
    @Path("/{id}")
    public UserTag update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid TagRequest req) {
        return switch (userTagService.update(id, req.name(), req.color())) {
            case Result.Success(var t) -> t;
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @DELETE
    @Path("/{id}")
    public Response delete(
            @PathParam("uuid") UUID uuid,
            @PathParam("id") UUID id,
            @QueryParam("strategy") @Nullable String strategy,
            @QueryParam("targetId") @Nullable UUID targetId
    ) {
        val parsed = Deletions.parse(strategy, targetId, ALLOWED_STRATEGIES);
        if (parsed instanceof Result.Failure<DeletionStrategy, BusinessError>(var error)) {
            throw new BusinessException(error);
        }

        return executeDelete(uuid, id, parsed.get(), targetId);
    }

    private Response executeDelete(UUID uuid, UUID id, @Nullable DeletionStrategy deletionStrategy, @Nullable UUID targetId) {
        if (deletionStrategy == null) {
            val count = userTagService.linkedTransactionIds(uuid, id).size();
            if (count > 0) {
                return Deletions.linkedConflict(null, count, "Existem " + count + " transações vinculadas a esta tag.");
            }
            return finish(userTagService.deleteById(id));
        }

        val result = switch (deletionStrategy) {
            case MOVE -> userTagService.deleteMoving(id, Objects.requireNonNull(targetId), uuid);
            case DELETE -> userTagService.deleteWithTransactions(id, uuid);
            case DETACH -> userTagService.deleteDetached(id, uuid);
        };
        return finish(result);
    }

    private static Response finish(Result<Void, BusinessError> result) {
        if (result instanceof Result.Failure<Void, BusinessError>(var error)) {
            throw new BusinessException(error);
        }
        return Response.noContent().build();
    }
}
