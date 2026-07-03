package br.community.feature.user.categories;

import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.categories.core.CategoryResponse;
import br.community.feature.user.categories.core.CreateRequest;
import br.community.feature.user.categories.core.UpdateRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jboss.resteasy.reactive.RestResponse;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@Path("/api/{uuid}/categories")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CategoryResource {

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
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var existing) -> {
                if (existing.isSystem()) {
                    throw new DomainException(new DomainError.BusinessRule("Categoria de sistema não pode ser modificada"));
                }
                guardResult(userCategoryService.validateUniqueName(uuid, existing.nature().name(), req.name(), req.parentId(), id));
                yield CategoryResponse.from(userCategoryService.update(id, req.name(), req.parentId()));
            }
        };
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("uuid") UUID uuid, @PathParam("id") UUID id) {
        switch (userCategoryService.deleteById(id, uuid)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }

    private static void guardResult(Result<Void, DomainError> result) {
        if (result instanceof Result.Failure<Void, DomainError>(var error)) {
            throw new DomainException(error);
        }
    }
}
