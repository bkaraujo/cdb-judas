package br.cdb.feature.finance.categories;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.finance.deletion.DeletionStrategy;
import br.cdb.feature.finance.deletion.Deletions;
import br.cdb.feature.user.UserUseCase;
import br.commons.Result;
import br.commons.business.BusinessException;
import br.commons.tools.Strings;
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
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/categories")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CategoryResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final UserUseCase userUseCase;

    @GET
    public List<CategoryResponse> listAll(@PathParam("uuid") UUID uuid) {
        return userUseCase.categories(uuid).stream().map(CategoryResponse::from).toList();
    }

    @POST
    public RestResponse<CategoryResponse> create(@PathParam("uuid") UUID uuid, @Valid CreateRequest req) {
        val nature = Transaction.Type.valueOf(Strings.upper(req.nature()));
        return switch (userUseCase.createCategory(uuid, req.name(), nature, req.parentId())) {
            case Result.Success(var category) -> RestResponse.status(RestResponse.Status.CREATED, CategoryResponse.from(category));
            case Result.Failure(var error) -> throw new BusinessException(error);
        };
    }

    @PATCH
    @Path("/{id}")
    public CategoryResponse update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid UpdateRequest req) {
        return switch (userUseCase.updateCategory(uuid, id, req.name(), req.parentId(), req.active())) {
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
                parsed -> userUseCase.deleteCategory(uuid, id, parsed, targetId),
                "a esta categoria.");
    }
}
