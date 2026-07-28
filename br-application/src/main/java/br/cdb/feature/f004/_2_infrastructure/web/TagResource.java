package br.cdb.feature.f004._2_infrastructure.web;

import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f004._0_domain.UserTag;
import br.cdb.feature.f004._1_application.TagUseCase;
import br.cdb.feature.f004._2_infrastructure.web.request.TagRequest;
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

    private final TagUseCase tagUseCase;

    @GET
    public List<UserTag> listAll(@PathParam("uuid") UUID uuid) {
        return tagUseCase.tags(uuid);
    }

    @POST
    public RestResponse<UserTag> create(@PathParam("uuid") UUID uuid, @Valid TagRequest req) {
        return RestResponse.status(RestResponse.Status.CREATED, tagUseCase.createTag(uuid, req.name(), req.color()));
    }

    @PATCH
    @Path("/{id}")
    public UserTag update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid TagRequest req) {
        return switch (tagUseCase.updateTag(id, req.name(), req.color())) {
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
                parsed -> tagUseCase.deleteTag(uuid, id, parsed, targetId),
                "a esta tag.");
    }
}
