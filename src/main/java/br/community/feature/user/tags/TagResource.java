package br.community.feature.user.tags;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.tags.core.TagRequest;
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
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/tags")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TagResource {

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
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id) {
        switch (userTagService.deleteById(id)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }
}
