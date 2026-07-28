package br.cdb.feature.f004._1_application;

import br.cdb.feature.f004._0_domain.UserTag;
import br.cdb.feature.f004._0_domain.UserTagRepository;
import br.cdb.feature.f004._0_domain.event.TagEvents;
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
public class UserTagService {

    private final UserTagRepository repo;
    private final UserTransactionTagService tagLinkService;

    public List<UserTag> findAll(UUID personId) {
        return repo.findAllByPerson(personId);
    }

    public Result<UserTag, BusinessError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserTag, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Tag not found: " + id)));
    }

    public UserTag create(UUID personId, String name, String color) {
        val saved = repo.save(new UserTag(UUID.randomUUID(), personId, name, color, null));
        MessageBus.submit(new TagEvents.Created(saved));
        return saved;
    }

    public Result<UserTag, BusinessError> update(UUID id, String name, String color) {
        return findById(id).map(existing -> {
            val saved = repo.save(new UserTag(id, existing.personId(), name, color, existing.createdAt()));
            MessageBus.submit(new TagEvents.Updated(saved));
            return saved;
        });
    }

    public List<UUID> linkedTransactionIds(UUID personId, UUID tagId) {
        return tagLinkService.findTransactionIdsByTag(personId, tagId);
    }

    /** Sem estratégia e sem vínculos: exclusão simples. */
    public Result<Void, BusinessError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            repo.deleteById(id);
            MessageBus.submit(new TagEvents.Deleted(id, existing.personId()));
            return null;
        });
    }

    /** Re-key dos vínculos para {@code targetId} (MOVE) — imperativo, feito antes da remoção da tag
     *  de origem, que fica a cargo de {@code TagDeletedListener} (reage a {@code TagDeleted}). */
    public Result<Void, BusinessError> reassignMoving(UUID id, UUID targetId, UUID personId) {
        return findById(id).flatMap(ignoredSource -> findById(targetId).flatMap(target -> {
            if (target.id().equals(id)) {
                return Result.<Void>failure(new BusinessError.BusinessRule("Tag de destino deve ser diferente da origem"));
            }
            tagLinkService.reassignTag(id, targetId, personId);
            return Result.success();
        }));
    }
}
