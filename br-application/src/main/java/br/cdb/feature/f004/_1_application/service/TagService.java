package br.cdb.feature.f004._1_application.service;

import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagService {

    private final TagRepository repository;
    private final TransactionTagService tagLinkService;

    public List<Tag> findAll(UUID personId) {
        return repository.findAllByPerson(personId);
    }

    public Result<Tag, BusinessError> find(UUID personId, UUID tagId) {
        return repository.findByPersonAndId(personId, tagId)
                .<Result<Tag, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Tag not found: " + tagId)));
    }

    public Result<Tag, BusinessError> findById(UUID id) {
        return repository.findById(id)
                .<Result<Tag, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Tag not found: " + id)));
    }

    public Tag create(UUID personId, String name, String color) {
        val saved = repository.save(new Tag(UUID.randomUUID(), personId, name, color, null));
        MessageBus.submit(new TagEvents.Created(saved));
        return saved;
    }

    public Result<Tag, BusinessError> update(UUID id, String name, String color) {
        return findById(id).map(existing -> {
            val saved = repository.save(new Tag(id, existing.personId(), name, color, existing.createdAt()));
            MessageBus.submit(new TagEvents.Updated(saved));
            return saved;
        });
    }

    /** Sem estratégia e sem vínculos: exclusão simples. */
    public Result<Void, BusinessError> deleteById(UUID id) {
        repository.deleteById(id);
        return Result.success();
    }
}
