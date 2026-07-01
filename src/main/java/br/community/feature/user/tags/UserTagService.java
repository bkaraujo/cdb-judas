package br.community.feature.user.tags;

import br.commons.Result;
import br.community.context.shared._0_domain.model.DomainError;
import br.community.feature.user.stream.SSE;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTagService {

    private static final String TYPE = "TAG";

    private final UserTagRepository repo;
    private final SSE sse;

    public List<UserTag> findAll(UUID userId) {
        return repo.findAllByUser(userId);
    }

    public Result<UserTag, DomainError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserTag, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Tag not found: " + id)));
    }

    public UserTag create(UUID userId, String name, String color) {
        val saved = repo.save(new UserTag(UUID.randomUUID(), userId, name, color, null));
        upsert(saved);
        return saved;
    }

    public Result<UserTag, DomainError> update(UUID id, String name, String color) {
        return findById(id).map(existing -> {
            val saved = repo.save(new UserTag(id, existing.userId(), name, color, existing.createdAt()));
            upsert(saved);
            return saved;
        });
    }

    public Result<Void, DomainError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            repo.deleteById(id);
            delete(existing.userId(), id);
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
