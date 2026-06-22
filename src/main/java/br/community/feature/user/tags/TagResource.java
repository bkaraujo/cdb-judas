package br.community.feature.user.tags;

import br.commons.Result;
import br.community.context.shared._1_application.DomainException;
import br.community.feature.user.tags.core.TagRequest;
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

    private final UserTagService userTagService;

    @GetMapping
    public List<UserTag> listAll(@PathVariable UUID uuid) {
        return userTagService.findAll(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserTag create(@PathVariable UUID uuid, @RequestBody @Valid TagRequest req) {
        return userTagService.create(uuid, req.name(), req.color());
    }

    @PatchMapping("/{id}")
    public UserTag update(@PathVariable UUID uuid, @PathVariable UUID id, @RequestBody @Valid TagRequest req) {
        return switch (userTagService.update(id, req.name(), req.color())) {
            case Result.Success(var t) -> t;
            case Result.Failure(var error) -> throw new DomainException(error);
        };
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid, @PathVariable UUID id) {
        switch (userTagService.deleteById(id)) {
            case Result.Success(var ignored) -> {}
            case Result.Failure(var error) -> throw new DomainException(error);
        }
    }
}
