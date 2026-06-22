package br.community.feature.user.categories;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserCategoryRepository {
    List<UserCategory> findAllByUser(UUID userId);
    Optional<UserCategory> findById(UUID id);
    UserCategory save(UserCategory category);
    void deleteById(UUID id);
}
