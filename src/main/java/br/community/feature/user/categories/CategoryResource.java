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
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryResource {

    private final UserCategoryService userCategoryService;

    @GetMapping
    public List<CategoryResponse> listAll(@PathVariable UUID uuid) {
        return userCategoryService.findAll(uuid).stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@PathVariable UUID uuid, @RequestBody @Valid CreateRequest req) {
        val nature = Transaction.Type.valueOf(Strings.upper(req.nature()));

        if (req.parentId() != null) {
            guardResult(userCategoryService.validateParent(req.parentId(), nature));
        }
        guardResult(userCategoryService.validateUniqueName(uuid, req.name(), req.parentId(), null));

        return CategoryResponse.from(userCategoryService.create(uuid, req.name(), nature, req.parentId()));
    }

    @PatchMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID uuid, @PathVariable UUID id, @RequestBody @Valid UpdateRequest req) {
        return switch (userCategoryService.findById(id)) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var existing) -> {
                if (existing.isSystem()) {
                    throw new DomainException(new DomainError.BusinessRule("Categoria de sistema não pode ser modificada"));
                }
                guardResult(userCategoryService.validateUniqueName(uuid, req.name(), req.parentId(), id));
                yield CategoryResponse.from(userCategoryService.update(id, req.name(), req.parentId()));
            }
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid, @PathVariable UUID id) {
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
