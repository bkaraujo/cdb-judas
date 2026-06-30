package br.community.feature.user.tags;

import br.commons.Result;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

@NullMarked
@Singleton
@RequiredArgsConstructor
public class UserTagService {

    private final UserTagRepository repo;

    public List<UserTag> findAll(UUID userId) {
        return repo.findAllByUser(userId);
    }

    public Result<UserTag, DomainError> findById(UUID id) {
        return repo.findById(id)
                .<Result<UserTag, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Tag not found: " + id)));
    }

    public UserTag create(UUID userId, String name, String color) {
        return repo.save(new UserTag(UUID.randomUUID(), userId, name, color, null));
    }

    public Result<UserTag, DomainError> update(UUID id, String name, String color) {
        return findById(id).map(existing ->
                repo.save(new UserTag(id, existing.userId(), name, color, existing.createdAt())));
    }

    public Result<Void, DomainError> deleteById(UUID id) {
        return findById(id).map(existing -> {
            repo.deleteById(id);
            return null;
        });
    }
}
