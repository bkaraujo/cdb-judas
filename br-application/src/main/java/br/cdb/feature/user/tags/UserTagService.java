package br.cdb.feature.user.tags;

import br.cdb.feature.user.stream.SSE;
import br.commons.Result;
import br.commons.business.BusinessError;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTagService {

    private static final String TYPE = "TAG";

    private final UserTagRepository repo;
    private final UserTransactionTagService tagLinkService;
    private final SSE sse;

    public List<UserTag> findAll(UUID userId) {
        return repo.findAllByUser(userId);
    }

    public Result<UserTag, BusinessError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserTag, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("Tag not found: " + id)));
    }

    public UserTag create(UUID userId, String name, String color) {
        val saved = repo.save(new UserTag(UUID.randomUUID(), userId, name, color, null));
        upsert(saved);
        return saved;
    }

    public Result<UserTag, BusinessError> update(UUID id, String name, String color) {
        return findById(id).map(existing -> {
            val saved = repo.save(new UserTag(id, existing.userId(), name, color, existing.createdAt()));
            upsert(saved);
            return saved;
        });
    }

    public List<UUID> linkedTransactionIds(UUID userId, UUID tagId) {
        return tagLinkService.findTransactionIdsByTag(userId, tagId);
    }

    /** Sem estratégia e sem vínculos: exclusão simples. */
    public Result<Void, BusinessError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            repo.deleteById(id);
            delete(existing.userId(), id);
            return null;
        });
    }

    public Result<Void, BusinessError> deleteMoving(UUID id, UUID targetId, UUID userId) {
        return findById(id).flatMap(ignoredSource -> findById(targetId).flatMap(target -> {
            if (target.id().equals(id)) {
                return Result.<Void>failure(new BusinessError.BusinessRule("Tag de destino deve ser diferente da origem"));
            }
            tagLinkService.reassignTag(id, targetId, userId);
            repo.deleteById(id);
            delete(userId, id);
            return Result.success();
        }));
    }

    /** Desvincula (apaga só a associação) e exclui a tag; transações permanecem intactas. */
    public Result<Void, BusinessError> deleteDetached(UUID id, UUID userId) {
        return findById(id).map(existing -> {
            tagLinkService.deleteByTag(userId, id);
            repo.deleteById(id);
            delete(userId, id);
            return null;
        });
    }

    @SuppressWarnings("EmptyCatch")
    private void upsert(UserTag tag) {
        try {
            sse.dispatch(tag.userId().toString(), SSE.Event.UPSERT, Map.of("type", TYPE, "payload", tag));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("EmptyCatch")
    private void delete(UUID userId, UUID tagId) {
        try {
            sse.dispatch(userId.toString(), SSE.Event.DELETE, Map.of("type", TYPE, "id", tagId.toString()));
        } catch (Exception ignored) {}
    }
}
