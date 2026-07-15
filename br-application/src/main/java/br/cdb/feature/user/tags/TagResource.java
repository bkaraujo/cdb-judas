package br.cdb.feature.user.tags;

import br.cdb.feature.user.UserUseCase;
import br.cdb.feature.user.deletion.DeletionStrategy;
import br.cdb.feature.user.deletion.Deletions;
import br.cdb.feature.user.tags.core.TagRequest;
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
@Path("/api/{uuid}/tags")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TagResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES =
            Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE, DeletionStrategy.DETACH);

    private final UserUseCase userUseCase;

    @GET
    public List<UserTag> listAll(@PathParam("uuid") UUID uuid) {
        return userUseCase.tags(uuid);
    }

    @POST
    public RestResponse<UserTag> create(@PathParam("uuid") UUID uuid, @Valid TagRequest req) {
        return RestResponse.status(RestResponse.Status.CREATED, userUseCase.createTag(uuid, req.name(), req.color()));
    }

    @PATCH
    @Path("/{id}")
    public UserTag update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid TagRequest req) {
        return switch (userUseCase.updateTag(id, req.name(), req.color())) {
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
        return Deletions.execute(strategy, targetId, ALLOWED_STRATEGIES,
                parsed -> userUseCase.deleteTag(uuid, id, parsed, targetId),
                "a esta tag.");
    }
}
