package br.cdb.feature.user.categories;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.user.categories.core.CategoryResponse;
import br.cdb.feature.user.categories.core.CreateRequest;
import br.cdb.feature.user.categories.core.UpdateRequest;
import br.cdb.feature.user.deletion.DeletionStrategy;
import br.cdb.feature.user.deletion.Deletions;
import br.commons.Result;
import br.commons.business.BusinessError;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/categories")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CategoryResource {

    private static final Set<DeletionStrategy> ALLOWED_STRATEGIES = Set.of(DeletionStrategy.MOVE, DeletionStrategy.DELETE);

    private final UserCategoryService userCategoryService;

    @GET
    public List<CategoryResponse> listAll(@PathParam("uuid") UUID uuid) {
        return userCategoryService.findAll(uuid).stream().map(CategoryResponse::from).toList();
    }

    @POST
    public RestResponse<CategoryResponse> create(@PathParam("uuid") UUID uuid, @Valid CreateRequest req) {
        val nature = Transaction.Type.valueOf(Strings.upper(req.nature()));

        if (req.parentId() != null) {
            guardResult(userCategoryService.validateParent(req.parentId(), nature));
        }
        guardResult(userCategoryService.validateUniqueName(uuid, req.nature(), req.name(), req.parentId(), null));

        return RestResponse.status(RestResponse.Status.CREATED, CategoryResponse.from(userCategoryService.create(uuid, req.name(), nature, req.parentId())));
    }

    @PATCH
    @Path("/{id}")
    public CategoryResponse update(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id, @Valid UpdateRequest req) {
        return switch (userCategoryService.findById(id)) {
            case Result.Failure(var error) -> throw new BusinessException(error);
            case Result.Success(var existing) -> {
                if (existing.isSystem()) {
                    throw new BusinessException(new BusinessError.BusinessRule("Categoria de sistema não pode ser modificada"));
                }
                guardResult(userCategoryService.validateUniqueName(uuid, existing.nature().name(), req.name(), req.parentId(), id));
                yield CategoryResponse.from(userCategoryService.update(id, req.name(), req.parentId(), req.active()));
            }
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
        val subtreeResult = userCategoryService.subtreeIds(id, uuid);
        if (subtreeResult instanceof Result.Failure<List<UUID>, BusinessError>(var error)) {
            throw new BusinessException(error);
        }
        val subtree = subtreeResult.get();

        val parsed = Deletions.parse(strategy, targetId, ALLOWED_STRATEGIES);
        if (parsed instanceof Result.Failure<DeletionStrategy, BusinessError>(var error)) {
            throw new BusinessException(error);
        }

        return executeDelete(uuid, id, subtree, parsed.get(), targetId);
    }

    private Response executeDelete(UUID uuid, UUID id, List<UUID> subtree, @Nullable DeletionStrategy deletionStrategy, @Nullable UUID targetId) {
        if (deletionStrategy == null) {
            val count = userCategoryService.linkedTransactionIds(subtree, uuid).size();
            if (count > 0) {
                return Deletions.linkedConflict(null, count, "Existem " + count + " transações vinculadas a esta categoria.");
            }
            userCategoryService.deletePlain(subtree, uuid);
            return Response.noContent().build();
        }

        val result = deletionStrategy == DeletionStrategy.MOVE
                ? userCategoryService.deleteMoving(id, Objects.requireNonNull(targetId), uuid)
                : userCategoryService.deleteWithTransactions(subtree, uuid);

        if (result instanceof Result.Failure<Void, BusinessError>(var error)) {
            throw new BusinessException(error);
        }
        return Response.noContent().build();
    }

    private static void guardResult(Result<Void, BusinessError> result) {
        if (result instanceof Result.Failure<Void, BusinessError>(var error)) {
            throw new BusinessException(error);
        }
    }
}
