package br.cdb.feature.f004._2_infrastructure.web;

import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f004.F004Api;
import br.cdb.feature.f004._1_application.usecase.ReadUseCase;
import br.cdb.feature.f004._1_application.usecase.WriteUseCase;
import br.cdb.feature.f004._2_infrastructure.web.request.TagRequest;
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
@Path("/api/{uuid}/tags")
@Produces(MediaType.APPLICATION_JSON)
public class TagResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DETACH);

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);
    private final WriteUseCase writes = Context.tryGet(WriteUseCase.class);

    @GET
    public List<F004Api.TagView> listAll(@PathParam("uuid") UUID uuid) {
        return reads.tags(uuid).stream().map(F004Api.TagView::from).toList();
    }

    @POST
    public RestResponse<F004Api.TagView> create(@PathParam("uuid") UUID uuid, @Valid TagRequest req) {
        return switch (writes.createTag(uuid, req.name(), req.color())) {
            case Result.Success(var t) -> RestResponse.status(RestResponse.Status.CREATED, F004Api.TagView.from(t));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public F004Api.TagView update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid TagRequest req) {
        return switch (writes.updateTag(id, req.name(), req.color())) {
            case Result.Success(var t) -> F004Api.TagView.from(t);
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
        return Deletions.execute(
                strategy,
                targetId,
                ALLOWED_STRATEGIES,
                parsed -> writes.deleteTag(uuid, id, parsed, targetId),
                "a esta tag."
        );
    }
}
