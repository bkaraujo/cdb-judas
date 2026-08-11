package br.cdb.feature.f004._1_application.service;

import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/** Context-wired como os demais serviços de fatia (o par {@code ReadUseCase}/{@code WriteUseCase} o
 *  resolve com {@code Context.tryGet}); sem CDI. */
@NullMarked
public class TagService {

    private final TagRepository repository = Context.get(TagRepository.class);

    public List<Tag> findAll(UUID personId) {
        return repository.findAllByPerson(personId);
    }

    public Result<Tag, BusinessError> find(UUID personId, UUID tagId) {
        return repository.findByPersonAndId(personId, tagId)
                .<Result<Tag, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Tag not found: %s", tagId)));
    }

    public Result<Tag, BusinessError> findById(UUID id) {
        return repository.findById(id)
                .<Result<Tag, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Tag not found: %s", id)));
    }

    public Result<Tag, BusinessError> create(UUID personId, String name, String color) {
        val duplicate = repository.findAllByPerson(personId).stream()
                .anyMatch(t -> t.name().trim().equalsIgnoreCase(name.trim()));
        if (duplicate) {
            return Result.failure(new BusinessError.Conflict("Já existe uma tag chamada \"%s\"", name.trim()));
        }
        val saved = repository.save(new Tag(UUID.randomUUID(), personId, name, color, null));
        MessageBus.submit(new TagEvents.Created(saved));
        return Result.success(saved);
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
