package br.community.feature.user.categories;

import br.commons.Result;
import br.commons.tools.Strings;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._1_application.command.CategoryCommand;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.categories.core.Category;
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

    private final MonetaryContext monetaryContext;

    @GetMapping
    public List<Category> listAll() {
        return switch (monetaryContext.listCategories()) {
            case Result.Success(var categories) -> categories.stream().map(Category::from).toList();
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@RequestBody @Valid CreateRequest req) {
        val nature = MonetaryTransaction.Type.valueOf(Strings.upper(req.nature()));
        val command = new CategoryCommand(req.name(), nature, req.parentId());

        return switch (monetaryContext.createCategory(command)) {
            case Result.Success(var c) -> Category.from(c);
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public Category update(@PathVariable UUID id, @RequestBody @Valid UpdateRequest req) {
        return switch (monetaryContext.findCategoryById(id)) {
            case Result.Failure(var error) -> throw new DomainException(error);
            case Result.Success(var existing) -> {
                val command = new CategoryCommand(req.name(), existing.nature(), req.parentId());
                yield switch (monetaryContext.updateCategory(id, command)) {
                    case Result.Success(var c) -> Category.from(c);
                    case Result.Failure(var error) -> throw new DomainException(error);
                };
            }
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        switch (monetaryContext.deleteCategory(id)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }
}
