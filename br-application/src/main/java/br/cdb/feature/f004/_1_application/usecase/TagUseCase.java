package br.cdb.feature.f004._1_application.usecase;

import br.cdb.feature.f000._0_domain.DeletionOutcome;
import br.cdb.feature.f000._0_domain.DeletionStrategy;
import br.cdb.feature.f004._0_domain.event.TagEvents;
import br.cdb.feature.f004._0_domain.model.Tag;
import br.cdb.feature.f004._1_application.service.TagService;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class TagUseCase {

    private final TagService service;

    public List<Tag> tags(UUID personId) {
        return service.findAll(personId);
    }

    public Tag createTag(UUID personId, String name, String color) {
        return service.create(personId, name, color);
    }

    public Result<Tag, BusinessError> updateTag(UUID id, String name, String color) {
        return service.update(id, name, color);
    }

    public Result<DeletionOutcome, BusinessError> deleteTag(UUID personId, UUID tagId, @Nullable DeletionStrategy strategy, @Nullable UUID targetId) {
        return switch (service.find(personId, tagId)) {
            case Result.Success(var tag) -> {
                val effectiveStrategy = strategy != null ? strategy : DeletionStrategy.DETACH;

                service.deleteById(tag.id());
                MessageBus.submit(new TagEvents.Deleted(tag, effectiveStrategy, targetId));
                yield Result.success(new DeletionOutcome.Completed());
            }
            case Result.Failure(var error) -> Result.failure(error);
        };
    }

}
