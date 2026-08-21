package br.cdb.feature.f005._2_infrastructure.web;

import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f000._1_application.Deletions;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._1_application.usecase.ReadUseCase;
import br.cdb.feature.f005._1_application.usecase.WriteUseCase;
import br.cdb.feature.f005._2_infrastructure.web.request.CreateRequest;
import br.cdb.feature.f005._2_infrastructure.web.request.UpdateRequest;
import br.cdb.feature.f005._2_infrastructure.web.response.CategoryResponse;
import br.cdb.feature.f005._2_infrastructure.web.response.TransferCategoryResponse;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.framework.cdi.Context;
import br.commons.tools.Strings;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final ReadUseCase reads = Context.tryGet(ReadUseCase.class);
    private final WriteUseCase writes = Context.tryGet(WriteUseCase.class);

    @GET
    public List<CategoryResponse> listAll(@PathParam("uuid") UUID uuid) {
        return reads.categories(uuid).stream().map(CategoryResponse::from).toList();
    }

    /** Endpoint interno (consumido pelo cliente {@code F005Api} da própria fatia, sobre
     *  {@code InternalApi}, por {@code f006.TransferUseCase} ao criar transferência) — público como
     *  qualquer outro em {@code /api/{uuid}/…}, guardado pelo mesmo {@code OwnershipFilter}, nunca
     *  chamado pelo frontend. */
    @GET
    @Path("/transfer")
    public TransferCategoryResponse transferCategory(@PathParam("uuid") UUID uuid, @QueryParam("nature") String nature) {
        val category = reads.transferCategory(uuid, Nature.valueOf(Strings.upper(nature)));
        return new TransferCategoryResponse(category.id());
    }

    @POST
    public RestResponse<CategoryResponse> create(@PathParam("uuid") UUID uuid, @Valid CreateRequest req) {
        val nature = Nature.valueOf(Strings.upper(req.nature()));
        return switch (writes.createCategory(uuid, req.name(), nature, req.parentId())) {
            case Result.Success(var category) -> RestResponse.status(RestResponse.Status.CREATED, CategoryResponse.from(category));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public CategoryResponse update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid UpdateRequest req) {
        return switch (writes.updateCategory(uuid, id, req.name(), req.parentId(), req.active())) {
            case Result.Success(var category) -> CategoryResponse.from(category);
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
                parsed -> writes.deleteCategory(uuid, id, parsed, targetId),
                "a esta categoria.");
    }
}
