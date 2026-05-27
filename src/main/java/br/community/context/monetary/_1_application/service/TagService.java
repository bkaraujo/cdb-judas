package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Tag;
import br.community.context.monetary._0_domain.repository.TagRepository;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    public Result<Tag, DomainError> findById(UUID id) {
        return tagRepository.findById(id)
                .<Result<Tag, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Tag not found: " + id)));
    }

    public Tag save(UUID id, String name, String color) {
        return tagRepository.save(new Tag(id, name, color));
    }

    public Result<Void, DomainError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            tagRepository.deleteById(id);
            return null;
        });
    }
}
