package br.community.feature.records.tags;

import br.commons.Result;
import br.community.context.monetary.MonetaryContext;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._1_application.command.TagCommand;
import br.community.context.shared._1_application.DomainException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/{uuid}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
public class TagResource {

    private final MonetaryContext monetaryContext;

    @GetMapping
    public List<Tag> listAll() {
        return switch (monetaryContext.listTags()) {
            case Result.Success(var list) -> list;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tag create(@RequestBody @Valid TagRequest req) {
        return switch (monetaryContext.createTag(new TagCommand(req.name(), req.color()))) {
            case Result.Success(var t) -> t;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @PatchMapping("/{id}")
    public Tag update(@PathVariable UUID id, @RequestBody @Valid TagRequest req) {
        return switch (monetaryContext.updateTag(id, new TagCommand(req.name(), req.color()))) {
            case Result.Success(var t) -> t;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        switch (monetaryContext.deleteTag(id)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }
}
